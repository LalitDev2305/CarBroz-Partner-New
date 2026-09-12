package com.carbroz.feature.dynamic

import com.carbroz.data.network.NetworkAuthentication
import com.carbroz.data.network.NetworkDataSource
import com.carbroz.data.network.NetworkMethod
import com.carbroz.data.network.NetworkRequest
import com.carbroz.data.network.NetworkResponse
import com.carbroz.data.network.NetworkResult
import com.carbroz.foundation.capabilities.CapabilityAvailability
import com.carbroz.foundation.capabilities.CapabilityKind
import com.carbroz.foundation.capabilities.CapabilityProvider
import com.carbroz.foundation.capabilities.CapabilityRegistry
import com.carbroz.foundation.capabilities.CapabilityRequest
import com.carbroz.foundation.capabilities.CapabilityResult
import com.carbroz.foundation.session.SessionPersistence
import com.carbroz.foundation.session.SessionPersistenceResult
import com.carbroz.foundation.session.SessionRestoreResult
import com.carbroz.foundation.session.SessionState
import com.carbroz.foundation.session.SessionStore
import com.carbroz.sdui.model.ExternalUriPayload
import com.carbroz.sdui.model.PresentPayload
import com.carbroz.sdui.model.RequestPayload
import com.carbroz.sdui.model.SequencePayload
import com.carbroz.sdui.model.SduiAction
import com.carbroz.sdui.model.SduiAuthentication
import com.carbroz.sdui.model.SduiDestination
import com.carbroz.sdui.model.SduiNavigationMode
import com.carbroz.sdui.model.SduiPresentationMode
import com.carbroz.sdui.model.SduiRequestMethod
import com.carbroz.sdui.model.SduiRequestResponseMode
import com.carbroz.sdui.model.SduiStateOperation
import com.carbroz.sdui.model.SduiStateProperty
import com.carbroz.sdui.model.StatePayload
import com.carbroz.sdui.value.SduiValueResolver
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull

class SduiActionExecutorTest {
    @Test
    fun requestResolvesValuesBuildsNetworkRequestAndStoresSuccessfulResponseAndContext() = runTest {
        val response = JsonObject(mapOf("data" to JsonObject(mapOf("ok" to JsonPrimitive(true)))))
        val network = RecordingNetworkDataSource(NetworkResult.Success(NetworkResponse(200, body = response)))
        val fixture = fixture(network)
        val action = SduiAction.Request(
            RequestPayload(
                method = SduiRequestMethod.POST,
                endpoint = "/api/v1/auth/login",
                authentication = SduiAuthentication.NONE,
                body = JsonObject(
                    mapOf(
                        "phone" to reference("\$binding", "phone"),
                        "deviceId" to reference("\$context", "deviceId"),
                    ),
                ),
                responseMode = SduiRequestResponseMode.NONE,
                contextUpdates = JsonObject(
                    mapOf(
                        "authFlow" to JsonObject(
                            mapOf("phone" to reference("\$binding", "phone")),
                        ),
                    ),
                ),
            ),
        )

        val result = fixture.executor.execute(action, mapOf("phone" to JsonPrimitive("9999999999")))

        assertIs<SduiActionResult.Completed>(result)
        val request = network.requests.single()
        assertEquals(NetworkMethod.POST, request.method)
        assertEquals("/api/v1/auth/login", request.endpoint.value)
        assertEquals(NetworkAuthentication.NONE, request.authentication)
        assertEquals(JsonPrimitive("9999999999"), request.payload?.get("phone"))
        assertEquals(JsonPrimitive("device-test"), request.payload?.get("deviceId"))
        val snapshot = fixture.flow.snapshot()
        assertEquals(response, snapshot.response)
        assertEquals(JsonPrimitive("9999999999"), snapshot.context["authFlow"]!!.jsonObject["phone"])
    }

    @Test
    fun failedRequestDoesNotApplyContextOrNavigate() = runTest {
        val network = RecordingNetworkDataSource(NetworkResult.Success(NetworkResponse(500, body = JsonObject(emptyMap()))))
        val fixture = fixture(network)
        val action = SduiAction.Request(
            RequestPayload(
                method = SduiRequestMethod.POST,
                endpoint = "/api/v1/fail",
                authentication = SduiAuthentication.NONE,
                responseMode = SduiRequestResponseMode.DESTINATION,
                contextUpdates = JsonObject(mapOf("shouldNotExist" to JsonPrimitive(true))),
            ),
        )

        val result = fixture.executor.execute(action, emptyMap())

        assertEquals(SduiActionResult.Failure("http_500"), result)
        val snapshot = fixture.flow.snapshot()
        assertEquals(JsonObject(emptyMap()), snapshot.context)
        assertNull(snapshot.response)
    }

    @Test
    fun destinationResponseEstablishesSessionBeforeReturningNavigation() = runTest {
        val response = JsonObject(
            mapOf(
                "data" to JsonObject(
                    mapOf(
                        "token" to JsonPrimitive("access-token"),
                        "refreshToken" to JsonPrimitive("refresh-token"),
                        "user" to JsonObject(mapOf("id" to JsonPrimitive("partner-1"))),
                        "nextScreen" to JsonObject(
                            mapOf(
                                "screenId" to JsonPrimitive("dashboard"),
                                "templateId" to JsonPrimitive("dashboard_template"),
                                "templateType" to JsonPrimitive("stack_template"),
                                "endpoint" to JsonPrimitive("/api/v1/screens/dashboard"),
                                "method" to JsonPrimitive("GET"),
                                "authentication" to JsonPrimitive("SESSION"),
                            ),
                        ),
                    ),
                ),
            ),
        )
        val network = RecordingNetworkDataSource(NetworkResult.Success(NetworkResponse(200, body = response)))
        val persistence = RecordingSessionPersistence()
        val fixture = fixture(network, persistence = persistence)
        val action = SduiAction.Request(
            RequestPayload(
                method = SduiRequestMethod.POST,
                endpoint = "/api/v1/auth/verify",
                authentication = SduiAuthentication.NONE,
                responseMode = SduiRequestResponseMode.DESTINATION,
                navigationMode = SduiNavigationMode.RESET,
            ),
        )

        val result = assertIs<SduiActionResult.Navigate>(fixture.executor.execute(action, emptyMap()))

        assertEquals("dashboard", result.destination.screenId)
        assertEquals(SduiNavigationMode.RESET, result.mode)
        val session = assertIs<SessionState.Authenticated>(fixture.sessionStore.current())
        assertEquals("partner-1", session.subject.value)
        assertEquals(session, persistence.saved)
    }

    @Test
    fun directNavigateUsesExactBackendDestinationAndMode() = runTest {
        val fixture = fixture(RecordingNetworkDataSource(successResponse()))
        val destination = SduiDestination(
            screenId = "booking_details",
            templateId = "booking_template",
            templateType = "stack_template",
            endpoint = "/api/v1/screens/booking/1",
            method = SduiRequestMethod.GET,
            authentication = SduiAuthentication.SESSION,
        )

        val result = assertIs<SduiActionResult.Navigate>(
            fixture.executor.execute(
                SduiAction.Navigate(destination, SduiNavigationMode.PUSH),
                emptyMap(),
            ),
        )

        assertEquals(DynamicDestination.from(destination), result.destination)
        assertEquals(SduiNavigationMode.PUSH, result.mode)
    }

    @Test
    fun presentDismissAndStateReturnStoreReducibleResults() = runTest {
        val fixture = fixture(RecordingNetworkDataSource(successResponse()))

        val present = assertIs<SduiActionResult.OverlayChanged>(
            fixture.executor.execute(
                SduiAction.Present("cancel_sheet", PresentPayload(SduiPresentationMode.BOTTOM_SHEET)),
                emptyMap(),
            ),
        )
        assertEquals("cancel_sheet", present.overlay?.targetId)
        assertEquals(SduiPresentationMode.BOTTOM_SHEET, present.overlay?.presentation)

        val dismiss = assertIs<SduiActionResult.OverlayChanged>(
            fixture.executor.execute(SduiAction.Dismiss(), emptyMap()),
        )
        assertNull(dismiss.overlay)

        val state = assertIs<SduiActionResult.NodeStateChanged>(
            fixture.executor.execute(
                SduiAction.State(
                    targetId = "resend",
                    payload = StatePayload(
                        operation = SduiStateOperation.SET,
                        property = SduiStateProperty.ENABLED,
                        value = JsonPrimitive(true),
                    ),
                ),
                emptyMap(),
            ),
        )
        assertEquals("resend", state.targetId)
        assertEquals(SduiStateProperty.ENABLED, state.update.property)
        assertEquals(JsonPrimitive(true), state.update.value)
    }

    @Test
    fun externalUriResolvesAndDelegatesToCapabilityRegistry() = runTest {
        val provider = RecordingExternalUriProvider()
        val fixture = fixture(
            RecordingNetworkDataSource(successResponse()),
            capabilities = CapabilityRegistry.builder().register(provider).build(),
        )
        fixture.flow.updateContext(
            JsonObject(mapOf("legal" to JsonObject(mapOf("termsUri" to JsonPrimitive("https://example.com/terms"))))),
        )

        val result = fixture.executor.execute(
            SduiAction.ExternalUri(ExternalUriPayload(reference("\$context", "legal.termsUri"))),
            emptyMap(),
        )

        assertIs<SduiActionResult.Completed>(result)
        assertEquals("https://example.com/terms", (provider.requests.single().arguments["uri"] as JsonPrimitive).content)
    }

    @Test
    fun unsafeExternalUriIsRejectedBeforeCapabilityExecution() = runTest {
        val provider = RecordingExternalUriProvider()
        val fixture = fixture(
            RecordingNetworkDataSource(successResponse()),
            capabilities = CapabilityRegistry.builder().register(provider).build(),
        )

        val result = fixture.executor.execute(
            SduiAction.ExternalUri(ExternalUriPayload(JsonPrimitive("javascript:alert(1)"))),
            emptyMap(),
        )

        assertEquals(SduiActionResult.Failure("external_uri_unsafe"), result)
        assertEquals(0, provider.requests.size)
    }

    @Test
    fun sequenceExecutesInOrderAndStopsOnFailure() = runTest {
        val network = RecordingNetworkDataSource(successResponse())
        val fixture = fixture(network)
        val sequence = SduiAction.Sequence(
            SequencePayload(
                listOf(
                    SduiAction.State(
                        "button",
                        StatePayload(SduiStateOperation.SET, SduiStateProperty.LOADING, JsonPrimitive(true)),
                    ),
                    SduiAction.Request(
                        RequestPayload(
                            method = SduiRequestMethod.POST,
                            endpoint = "/api/v1/action",
                            authentication = SduiAuthentication.NONE,
                            body = JsonObject(mapOf("required" to reference("\$binding", "missing"))),
                        ),
                    ),
                    SduiAction.Dismiss(),
                ),
            ),
        )

        val result = fixture.executor.execute(sequence, emptyMap())

        assertEquals(SduiActionResult.Failure("binding_missing:missing"), result)
        assertEquals(0, network.requests.size)
    }

    @Test
    fun latestSuccessfulRequestReplacesResponseSource() = runTest {
        val network = QueueNetworkDataSource(
            mutableListOf(
                NetworkResult.Success(NetworkResponse(200, body = JsonObject(mapOf("challenge" to JsonPrimitive("old"))))),
                NetworkResult.Success(NetworkResponse(200, body = JsonObject(mapOf("challenge" to JsonPrimitive("new"))))),
            ),
        )
        val fixture = fixture(network)
        val action = SduiAction.Request(
            RequestPayload(
                method = SduiRequestMethod.POST,
                endpoint = "/api/v1/resend",
                authentication = SduiAuthentication.NONE,
                responseMode = SduiRequestResponseMode.NONE,
            ),
        )

        fixture.executor.execute(action, emptyMap())
        fixture.executor.execute(action, emptyMap())

        assertEquals(JsonPrimitive("new"), fixture.flow.snapshot().response!!.jsonObject["challenge"])
    }

    private fun fixture(
        network: NetworkDataSource,
        persistence: RecordingSessionPersistence = RecordingSessionPersistence(),
        capabilities: CapabilityRegistry = CapabilityRegistry.builder().build(),
    ): Fixture {
        val flow = DynamicFlowContext()
        val sessionStore = SessionStore(persistence)
        val executor = SduiActionExecutor(
            network = network,
            capabilities = capabilities,
            values = SduiValueResolver(),
            contextProvider = DynamicContextProvider(
                flowContext = flow,
                staticContext = JsonObject(emptyMap()),
                deviceId = "device-test",
            ),
            flowContext = flow,
            sessionStore = sessionStore,
        )
        return Fixture(executor, flow, sessionStore)
    }

    private fun successResponse(): NetworkResult = NetworkResult.Success(
        NetworkResponse(200, body = JsonObject(mapOf("data" to JsonObject(emptyMap())))),
    )

    private fun reference(key: String, value: String): JsonObject = JsonObject(mapOf(key to JsonPrimitive(value)))

    private data class Fixture(
        val executor: SduiActionExecutor,
        val flow: DynamicFlowContext,
        val sessionStore: SessionStore,
    )

    private class RecordingNetworkDataSource(
        private val result: NetworkResult,
    ) : NetworkDataSource {
        val requests = mutableListOf<NetworkRequest>()
        override suspend fun execute(request: NetworkRequest): NetworkResult {
            requests += request
            return result
        }
    }

    private class QueueNetworkDataSource(
        private val results: MutableList<NetworkResult>,
    ) : NetworkDataSource {
        override suspend fun execute(request: NetworkRequest): NetworkResult = results.removeAt(0)
    }

    private class RecordingExternalUriProvider : CapabilityProvider {
        override val kind: CapabilityKind = CapabilityKind.EXTERNAL_URI
        override val availability: StateFlow<CapabilityAvailability> = MutableStateFlow(CapabilityAvailability.Available)
        val requests = mutableListOf<CapabilityRequest>()

        override suspend fun execute(request: CapabilityRequest): CapabilityResult {
            requests += request
            return CapabilityResult.Success()
        }
    }

    private class RecordingSessionPersistence : SessionPersistence {
        var saved: SessionState.Authenticated? = null

        override suspend fun restore(): SessionRestoreResult = SessionRestoreResult.NoSession

        override suspend fun save(session: SessionState.Authenticated): SessionPersistenceResult {
            saved = session
            return SessionPersistenceResult.Success
        }

        override suspend fun clear(): SessionPersistenceResult {
            saved = null
            return SessionPersistenceResult.Success
        }
    }
}
