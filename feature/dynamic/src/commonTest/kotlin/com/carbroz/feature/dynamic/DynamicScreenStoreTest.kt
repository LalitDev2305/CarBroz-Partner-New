package com.carbroz.feature.dynamic

import com.carbroz.data.network.NetworkDataSource
import com.carbroz.data.network.NetworkRequest
import com.carbroz.data.network.NetworkResponse
import com.carbroz.data.network.NetworkResult
import com.carbroz.foundation.capabilities.CapabilityRegistry
import com.carbroz.foundation.navigation.NavigationState
import com.carbroz.foundation.navigation.NavigationStore
import com.carbroz.foundation.session.SessionPersistence
import com.carbroz.foundation.session.SessionPersistenceResult
import com.carbroz.foundation.session.SessionRestoreResult
import com.carbroz.foundation.session.SessionState
import com.carbroz.foundation.session.SessionStore
import com.carbroz.sdui.model.PresentPayload
import com.carbroz.sdui.model.RequestPayload
import com.carbroz.sdui.model.SduiAction
import com.carbroz.sdui.model.SduiAuthentication
import com.carbroz.sdui.model.SduiComponent
import com.carbroz.sdui.model.SduiElement
import com.carbroz.sdui.model.SduiElementBinding
import com.carbroz.sdui.model.SduiPresentationMode
import com.carbroz.sdui.model.SduiRequestMethod
import com.carbroz.sdui.model.SduiRequestResponseMode
import com.carbroz.sdui.model.SduiScreen
import com.carbroz.sdui.model.SduiStateOperation
import com.carbroz.sdui.model.SduiStateProperty
import com.carbroz.sdui.model.SduiTargetApp
import com.carbroz.sdui.model.SduiTemplate
import com.carbroz.sdui.model.SduiValidationRule
import com.carbroz.sdui.model.StatePayload
import com.carbroz.sdui.parser.SduiDecoder
import com.carbroz.sdui.parser.SduiSupportChecker
import com.carbroz.sdui.registry.SduiNodeRegistration
import com.carbroz.sdui.render.SduiInteraction
import com.carbroz.sdui.value.SduiValueResolver
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class DynamicScreenStoreTest {
    @Test
    fun showUnwrapsEnvelopeChecksIdentityAndInitializesBoundFields() = runTest {
        val screen = screen()
        val network = QueueNetworkDataSource(mutableListOf(successEnvelope(screen)))
        val fixture = fixture(network)

        fixture.store.show(destination())
        advanceUntilIdle()

        val state = fixture.store.state.value
        assertFalse(state.loading)
        assertNull(state.failure)
        assertEquals(screen, state.screen)
        assertEquals(JsonPrimitive(""), state.fields["phone"]?.value)
        assertEquals(JsonPrimitive("keep"), state.fields["other"]?.value)
        assertEquals("device-test", (state.context["deviceId"] as JsonPrimitive).content)
        assertEquals("/api/v1/screens/login", network.requests.single().endpoint.value)
    }

    @Test
    fun destinationMismatchMalformedEnvelopeAndUnsupportedSchemaFailPredictably() = runTest {
        val mismatch = fixture(QueueNetworkDataSource(mutableListOf(successEnvelope(screen().copy(screenId = "different")))))
        mismatch.store.show(destination())
        advanceUntilIdle()
        assertEquals(
            DynamicScreenFailure.UnsupportedContract("destination_identity_mismatch"),
            mismatch.store.state.value.failure,
        )

        val malformed = fixture(
            QueueNetworkDataSource(
                mutableListOf(NetworkResult.Success(NetworkResponse(200, body = JsonObject(emptyMap())))),
            ),
        )
        malformed.store.show(destination())
        advanceUntilIdle()
        assertEquals(DynamicScreenFailure.Decode("screen_data_missing"), malformed.store.state.value.failure)

        val unsupported = fixture(
            QueueNetworkDataSource(mutableListOf(successEnvelope(screen().copy(schemaVersion = "99.0")))),
        )
        unsupported.store.show(destination())
        advanceUntilIdle()
        assertEquals(
            DynamicScreenFailure.UnsupportedContract("unsupported_schema:99.0"),
            unsupported.store.state.value.failure,
        )
    }

    @Test
    fun valueChangeUpdatesOnlyTargetField() = runTest {
        val fixture = loadedFixture()

        fixture.store.dispatch(DynamicScreenIntent.Interaction(valueChanged("9999999999")))

        val state = fixture.store.state.value
        assertEquals(JsonPrimitive("9999999999"), state.fields["phone"]?.value)
        assertTrue(state.fields["phone"]?.touched == true)
        assertNull(state.fields["phone"]?.error)
        assertEquals(JsonPrimitive("keep"), state.fields["other"]?.value)
    }

    @Test
    fun validationPolicyBlocksOnlyValidateTrueRequests() = runTest {
        val actionNetwork = RecordingNetworkDataSource(successActionResponse())
        val fixture = loadedFixture(actionNetwork)
        val validated = request(validate = true)

        fixture.store.dispatch(DynamicScreenIntent.Interaction(trigger(validated)))
        advanceUntilIdle()
        assertEquals(0, actionNetwork.requests.size)
        assertEquals("Enter 10 digits", fixture.store.state.value.fields["phone"]?.error)

        fixture.store.dispatch(DynamicScreenIntent.Interaction(valueChanged("9999999999")))
        fixture.store.dispatch(DynamicScreenIntent.Interaction(trigger(validated)))
        advanceUntilIdle()
        assertEquals(1, actionNetwork.requests.size)
        assertNull(fixture.store.state.value.fields["phone"]?.error)

        val secondNetwork = RecordingNetworkDataSource(successActionResponse())
        val unvalidatedFixture = loadedFixture(secondNetwork)
        unvalidatedFixture.store.dispatch(DynamicScreenIntent.Interaction(trigger(request(validate = false))))
        advanceUntilIdle()
        assertEquals(1, secondNetwork.requests.size)
    }

    @Test
    fun statePresentAndDismissReduceIntoSingleScreenState() = runTest {
        val fixture = loadedFixture()

        fixture.store.dispatch(
            DynamicScreenIntent.Interaction(
                trigger(
                    SduiAction.State(
                        targetId = "resend",
                        payload = StatePayload(
                            operation = SduiStateOperation.SET,
                            property = SduiStateProperty.ENABLED,
                            value = JsonPrimitive(true),
                        ),
                    ),
                ),
            ),
        )
        advanceUntilIdle()
        assertEquals(true, fixture.store.state.value.nodeStates["resend"]?.enabled)

        fixture.store.dispatch(
            DynamicScreenIntent.Interaction(
                trigger(SduiAction.Present("cancel_sheet", PresentPayload(SduiPresentationMode.BOTTOM_SHEET))),
            ),
        )
        advanceUntilIdle()
        assertEquals("cancel_sheet", fixture.store.state.value.overlay?.targetId)

        fixture.store.dispatch(DynamicScreenIntent.Interaction(trigger(SduiAction.Dismiss())))
        advanceUntilIdle()
        assertNull(fixture.store.state.value.overlay)
    }

    @Test
    fun refreshBackAndRetryUseCurrentFullDestinationAndNavigationStore() = runTest {
        val current = destination()
        val root = destination("root", "root_template", "/api/v1/screens/root")
        val network = QueueNetworkDataSource(
            mutableListOf(
                NetworkResult.Success(NetworkResponse(500, body = JsonObject(emptyMap()))),
                successEnvelope(screen()),
                successEnvelope(screen()),
            ),
        )
        val navigation = NavigationStore(NavigationState(listOf(root, current)))
        val fixture = fixture(network, navigation = navigation)

        fixture.store.show(current)
        advanceUntilIdle()
        assertIs<DynamicScreenFailure.Network>(fixture.store.state.value.failure)

        fixture.store.dispatch(DynamicScreenIntent.Retry)
        advanceUntilIdle()
        assertNull(fixture.store.state.value.failure)

        fixture.store.dispatch(DynamicScreenIntent.Refresh)
        advanceUntilIdle()
        assertEquals(3, network.requests.size)
        network.requests.forEach { assertEquals(current.endpoint, it.endpoint.value) }

        fixture.store.dispatch(DynamicScreenIntent.BackRequested)
        assertEquals(root, navigation.state.value.current)
    }

    @Test
    fun suspendForBackgroundCancelsOwnedLoadAndClearsFlags() = runTest {
        val network = CancellingNetworkDataSource()
        val fixture = fixture(network)

        fixture.store.show(destination())
        runCurrent()
        assertTrue(fixture.store.state.value.loading)

        fixture.store.suspendForBackground()
        advanceUntilIdle()

        assertTrue(network.cancelled)
        assertFalse(fixture.store.state.value.loading)
        assertFalse(fixture.store.state.value.actionInFlight)
    }

    private suspend fun kotlinx.coroutines.test.TestScope.loadedFixture(
        actionNetwork: NetworkDataSource = RecordingNetworkDataSource(successActionResponse()),
    ): Fixture {
        val fixture = fixture(
            QueueNetworkDataSource(mutableListOf(successEnvelope(screen()))),
            actionNetwork = actionNetwork,
        )
        fixture.store.show(destination())
        advanceUntilIdle()
        return fixture
    }

    private fun kotlinx.coroutines.test.TestScope.fixture(
        screenNetwork: NetworkDataSource,
        actionNetwork: NetworkDataSource = RecordingNetworkDataSource(successActionResponse()),
        navigation: NavigationStore = NavigationStore(NavigationState(listOf(destination()))),
    ): Fixture {
        val flow = DynamicFlowContext()
        val contextProvider = DynamicContextProvider(flow, deviceId = "device-test")
        val actionExecutor = SduiActionExecutor(
            network = actionNetwork,
            capabilities = CapabilityRegistry.builder().build(),
            values = SduiValueResolver(),
            contextProvider = contextProvider,
            flowContext = flow,
            sessionStore = SessionStore(InMemorySessionPersistence()),
        )
        return Fixture(
            DynamicScreenStore(
                scope = this,
                decoder = SduiDecoder(),
                supportChecker = SduiSupportChecker(SduiNodeRegistration.createRegistry()),
                network = screenNetwork,
                actionExecutor = actionExecutor,
                contextProvider = contextProvider,
                flowContext = flow,
                navigation = navigation,
            ),
        )
    }

    private fun request(validate: Boolean): SduiAction.Request = SduiAction.Request(
        RequestPayload(
            method = SduiRequestMethod.POST,
            endpoint = "/api/v1/auth/login",
            authentication = SduiAuthentication.NONE,
            validate = validate,
            body = JsonObject(
                mapOf("phone" to JsonObject(mapOf("\$binding" to JsonPrimitive("phone")))),
            ),
            responseMode = SduiRequestResponseMode.NONE,
        ),
    )

    private fun valueChanged(value: String): SduiInteraction.ValueChanged = SduiInteraction.ValueChanged(
        elementId = "phone_input",
        bindingKey = "phone",
        value = JsonPrimitive(value),
    )

    private fun trigger(action: SduiAction): SduiInteraction.ActionTriggered = SduiInteraction.ActionTriggered(
        sourceId = "test_source",
        event = "click",
        action = action,
    )

    private fun destination(
        screenId: String = "login",
        templateId: String = "login_template",
        endpoint: String = "/api/v1/screens/login",
    ): DynamicDestination = DynamicDestination(
        screenId = screenId,
        templateId = templateId,
        templateType = "form_template",
        endpoint = endpoint,
        method = SduiRequestMethod.GET,
        authentication = SduiAuthentication.NONE,
    )

    private fun screen(): SduiScreen = SduiScreen(
        screenId = "login",
        schemaVersion = "3.0",
        targetApp = SduiTargetApp.PARTNER,
        template = SduiTemplate(
            id = "login_template",
            type = "form_template",
            components = listOf(
                SduiComponent(
                    id = "form",
                    type = "stack_component",
                    elements = listOf(
                        SduiElement(
                            id = "phone_input",
                            type = "input",
                            properties = JsonObject(mapOf("value" to JsonPrimitive(""))),
                            validation = SduiValidationRule(
                                required = true,
                                pattern = "^[0-9]{10}$",
                                message = "Enter 10 digits",
                            ),
                            binding = SduiElementBinding("phone"),
                        ),
                        SduiElement(
                            id = "other_input",
                            type = "input",
                            properties = JsonObject(mapOf("value" to JsonPrimitive("keep"))),
                            binding = SduiElementBinding("other"),
                        ),
                    ),
                ),
            ),
        ),
    )

    private fun successEnvelope(screen: SduiScreen): NetworkResult {
        val data = Json.parseToJsonElement(Json.encodeToString(screen))
        return NetworkResult.Success(NetworkResponse(200, body = JsonObject(mapOf("data" to data))))
    }

    private fun successActionResponse(): NetworkResult = NetworkResult.Success(
        NetworkResponse(200, body = JsonObject(mapOf("data" to JsonObject(emptyMap())))),
    )

    private data class Fixture(val store: DynamicScreenStore)

    private class QueueNetworkDataSource(
        private val results: MutableList<NetworkResult>,
    ) : NetworkDataSource {
        val requests = mutableListOf<NetworkRequest>()
        override suspend fun execute(request: NetworkRequest): NetworkResult {
            requests += request
            return results.removeAt(0)
        }
    }

    private class RecordingNetworkDataSource(
        private val result: NetworkResult,
    ) : NetworkDataSource {
        val requests = mutableListOf<NetworkRequest>()
        override suspend fun execute(request: NetworkRequest): NetworkResult {
            requests += request
            return result
        }
    }

    private class CancellingNetworkDataSource : NetworkDataSource {
        var cancelled: Boolean = false
        override suspend fun execute(request: NetworkRequest): NetworkResult = try {
            kotlinx.coroutines.awaitCancellation()
        } catch (cancellation: CancellationException) {
            cancelled = true
            throw cancellation
        }
    }

    private class InMemorySessionPersistence : SessionPersistence {
        private var session: SessionState.Authenticated? = null
        override suspend fun restore(): SessionRestoreResult =
            session?.let(SessionRestoreResult::Restored) ?: SessionRestoreResult.NoSession

        override suspend fun save(session: SessionState.Authenticated): SessionPersistenceResult {
            this.session = session
            return SessionPersistenceResult.Success
        }

        override suspend fun clear(): SessionPersistenceResult {
            session = null
            return SessionPersistenceResult.Success
        }
    }
}
