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
import com.carbroz.sdui.model.RequestPayload
import com.carbroz.sdui.model.SduiAction
import com.carbroz.sdui.model.SduiAuthentication
import com.carbroz.sdui.model.SduiComponent
import com.carbroz.sdui.model.SduiElement
import com.carbroz.sdui.model.SduiElementBinding
import com.carbroz.sdui.model.SduiRequestMethod
import com.carbroz.sdui.model.SduiRequestResponseMode
import com.carbroz.sdui.model.SduiScreen
import com.carbroz.sdui.model.SduiStateOperation
import com.carbroz.sdui.model.SduiStateProperty
import com.carbroz.sdui.model.SduiTargetApp
import com.carbroz.sdui.model.SduiTemplate
import com.carbroz.sdui.model.StatePayload
import com.carbroz.sdui.parser.SduiDecoder
import com.carbroz.sdui.parser.SduiSupportChecker
import com.carbroz.sdui.registry.SduiNodeRegistration
import com.carbroz.sdui.render.SduiInteraction
import com.carbroz.sdui.value.SduiValueResolver
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.awaitCancellation
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
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class DynamicScreenStoreFrozenRegressionTest {
    @Test
    fun destinationTemplateIdAndTemplateTypeMismatchFailWithoutGuessing() = runTest {
        val templateIdMismatch = fixture(
            screenNetwork = QueueNetworkDataSource(
                mutableListOf(successEnvelope(screen().copy(template = screen().template.copy(id = "wrong_template")))),
            ),
        )
        templateIdMismatch.show(destination())
        advanceUntilIdle()
        assertEquals(
            DynamicScreenFailure.UnsupportedContract("destination_identity_mismatch"),
            templateIdMismatch.state.value.failure,
        )

        val templateTypeMismatch = fixture(
            screenNetwork = QueueNetworkDataSource(
                mutableListOf(successEnvelope(screen().copy(template = screen().template.copy(type = "stack_template")))),
            ),
        )
        templateTypeMismatch.show(destination())
        advanceUntilIdle()
        assertEquals(
            DynamicScreenFailure.UnsupportedContract("destination_identity_mismatch"),
            templateTypeMismatch.state.value.failure,
        )
    }

    @Test
    fun runtimeNodeStateDoesNotMutateImmutableServerScreen() = runTest {
        val store = loadedStore()
        val original = store.state.value.screen!!

        store.dispatch(
            DynamicScreenIntent.Interaction(
                SduiInteraction.ActionTriggered(
                    sourceId = "test",
                    event = "onClick",
                    action = SduiAction.State(
                        targetId = "phone_input",
                        payload = StatePayload(
                            operation = SduiStateOperation.SET,
                            property = SduiStateProperty.VALUE,
                            value = JsonPrimitive("runtime-value"),
                        ),
                    ),
                ),
            ),
        )
        advanceUntilIdle()

        assertEquals(original, store.state.value.screen)
        assertEquals(
            JsonPrimitive("server-value"),
            original.template.components.single().elements.orEmpty().single().properties["value"],
        )
        assertEquals(JsonPrimitive("runtime-value"), store.state.value.nodeStates["phone_input"]?.value)
    }

    @Test
    fun repeatedActionIsIgnoredWhilePreviousActionIsInFlight() = runTest {
        val actionNetwork = BlockingNetworkDataSource()
        val store = loadedStore(actionNetwork)
        val request = SduiAction.Request(
            RequestPayload(
                method = SduiRequestMethod.POST,
                endpoint = "/api/v1/action",
                authentication = SduiAuthentication.NONE,
                validate = false,
                responseMode = SduiRequestResponseMode.NONE,
            ),
        )
        val interaction = DynamicScreenIntent.Interaction(
            SduiInteraction.ActionTriggered("button", "onClick", request),
        )

        store.dispatch(interaction)
        runCurrent()
        assertTrue(store.state.value.actionInFlight)
        assertEquals(1, actionNetwork.requests.size)

        store.dispatch(interaction)
        runCurrent()
        assertEquals(1, actionNetwork.requests.size)

        store.suspendForBackground()
        advanceUntilIdle()
        assertFalse(store.state.value.actionInFlight)
    }

    private suspend fun kotlinx.coroutines.test.TestScope.loadedStore(
        actionNetwork: NetworkDataSource = ImmediateNetworkDataSource(),
    ): DynamicScreenStore {
        val store = fixture(
            screenNetwork = QueueNetworkDataSource(mutableListOf(successEnvelope(screen()))),
            actionNetwork = actionNetwork,
        )
        store.show(destination())
        advanceUntilIdle()
        return store
    }

    private fun kotlinx.coroutines.test.TestScope.fixture(
        screenNetwork: NetworkDataSource,
        actionNetwork: NetworkDataSource = ImmediateNetworkDataSource(),
    ): DynamicScreenStore {
        val flow = DynamicFlowContext()
        val context = DynamicContextProvider(flow, deviceId = "device-test")
        val navigation = NavigationStore(NavigationState(listOf(destination())))
        val executor = SduiActionExecutor(
            network = actionNetwork,
            capabilities = CapabilityRegistry.builder().build(),
            values = SduiValueResolver(),
            contextProvider = context,
            flowContext = flow,
            sessionStore = SessionStore(InMemorySessionPersistence()),
        )
        return DynamicScreenStore(
            scope = this,
            decoder = SduiDecoder(),
            supportChecker = SduiSupportChecker(SduiNodeRegistration.createRegistry()),
            network = screenNetwork,
            actionExecutor = executor,
            contextProvider = context,
            flowContext = flow,
            navigation = navigation,
        )
    }

    private fun destination(): DynamicDestination = DynamicDestination(
        screenId = "login",
        templateId = "login_template",
        templateType = "form_template",
        endpoint = "/api/v1/screens/login",
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
                    id = "root",
                    type = "stack_component",
                    elements = listOf(
                        SduiElement(
                            id = "phone_input",
                            type = "input",
                            properties = JsonObject(mapOf("value" to JsonPrimitive("server-value"))),
                            binding = SduiElementBinding("phone"),
                        ),
                    ),
                ),
            ),
        ),
    )

    private fun successEnvelope(screen: SduiScreen): NetworkResult = NetworkResult.Success(
        NetworkResponse(
            statusCode = 200,
            body = JsonObject(mapOf("data" to Json.parseToJsonElement(Json.encodeToString(screen)))),
        ),
    )

    private class QueueNetworkDataSource(
        private val results: MutableList<NetworkResult>,
    ) : NetworkDataSource {
        override suspend fun execute(request: NetworkRequest): NetworkResult = results.removeAt(0)
    }

    private class ImmediateNetworkDataSource : NetworkDataSource {
        override suspend fun execute(request: NetworkRequest): NetworkResult = NetworkResult.Success(
            NetworkResponse(200, body = JsonObject(mapOf("data" to JsonObject(emptyMap())))),
        )
    }

    private class BlockingNetworkDataSource : NetworkDataSource {
        val requests = mutableListOf<NetworkRequest>()
        override suspend fun execute(request: NetworkRequest): NetworkResult {
            requests += request
            awaitCancellation()
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
