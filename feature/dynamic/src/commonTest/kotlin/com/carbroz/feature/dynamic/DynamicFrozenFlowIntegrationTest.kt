package com.carbroz.feature.dynamic

import com.carbroz.data.network.NetworkDataSource
import com.carbroz.data.network.NetworkRequest
import com.carbroz.data.network.NetworkResponse
import com.carbroz.data.network.NetworkResult
import com.carbroz.foundation.capabilities.CapabilityRegistry
import com.carbroz.foundation.navigation.NavigationCommand
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
import com.carbroz.sdui.model.SduiDestination
import com.carbroz.sdui.model.SduiNavigationMode
import com.carbroz.sdui.model.SduiRequestMethod
import com.carbroz.sdui.model.SduiRequestResponseMode
import com.carbroz.sdui.value.SduiValueResolver
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class DynamicFrozenFlowIntegrationTest {
    @Test
    fun loginOtpDashboardFlow_isDrivenOnlyByGenericRequestResponses() = runTest {
        val network = QueueNetworkDataSource(
            mutableListOf(
                response(
                    nextScreen = destinationJson(
                        screenId = "auth_otp",
                        templateId = "auth_otp_template",
                        templateType = "form_template",
                        endpoint = "/api/v1/screens/auth/otp",
                        authentication = "NONE",
                    ),
                    extraData = mapOf("challengeId" to JsonPrimitive("challenge-1")),
                ),
                response(
                    nextScreen = destinationJson(
                        screenId = "partner_dashboard",
                        templateId = "dashboard_template",
                        templateType = "stack_template",
                        endpoint = "/api/v1/screens/dashboard",
                        authentication = "SESSION",
                    ),
                    extraData = mapOf(
                        "token" to JsonPrimitive("access-token"),
                        "refreshToken" to JsonPrimitive("refresh-token"),
                        "user" to JsonObject(mapOf("id" to JsonPrimitive("partner-1"))),
                    ),
                ),
            ),
        )
        val persistence = InMemorySessionPersistence()
        val flow = DynamicFlowContext()
        val executor = SduiActionExecutor(
            network = network,
            capabilities = CapabilityRegistry.builder().build(),
            values = SduiValueResolver(),
            contextProvider = DynamicContextProvider(flow, deviceId = "device-test"),
            flowContext = flow,
            sessionStore = SessionStore(persistence),
        )

        val loginResult = assertIs<SduiActionResult.Navigate>(
            executor.execute(
                request("/api/v1/auth/login", SduiNavigationMode.PUSH),
                mapOf("phone" to JsonPrimitive("9999999999")),
            ),
        )
        assertEquals("auth_otp", loginResult.destination.screenId)
        assertEquals("form_template", loginResult.destination.templateType)
        assertEquals(SduiNavigationMode.PUSH, loginResult.mode)
        assertEquals(JsonPrimitive("challenge-1"), flow.snapshot().response?.get("data")?.let { (it as JsonObject)["challengeId"] })

        val otpResult = assertIs<SduiActionResult.Navigate>(
            executor.execute(
                request("/api/v1/auth/verify", SduiNavigationMode.RESET),
                mapOf("otp" to JsonPrimitive("123456")),
            ),
        )
        assertEquals("partner_dashboard", otpResult.destination.screenId)
        assertEquals("stack_template", otpResult.destination.templateType)
        assertEquals(SduiNavigationMode.RESET, otpResult.mode)
        assertIs<SessionState.Authenticated>(persistence.session)
    }

    @Test
    fun dashboardBookingDetailsBackFlow_usesFullDestinationAndNavigationStorePop() = runTest {
        val dashboard = DynamicDestination(
            screenId = "partner_dashboard",
            templateId = "dashboard_template",
            templateType = "stack_template",
            endpoint = "/api/v1/screens/dashboard",
            method = SduiRequestMethod.GET,
            authentication = SduiAuthentication.SESSION,
        )
        val booking = SduiDestination(
            screenId = "booking_details",
            templateId = "booking_details_template",
            templateType = "default_template",
            endpoint = "/api/v1/screens/booking/1",
            method = SduiRequestMethod.GET,
            authentication = SduiAuthentication.SESSION,
        )
        val flow = DynamicFlowContext()
        val executor = SduiActionExecutor(
            network = QueueNetworkDataSource(mutableListOf()),
            capabilities = CapabilityRegistry.builder().build(),
            values = SduiValueResolver(),
            contextProvider = DynamicContextProvider(flow, deviceId = "device-test"),
            flowContext = flow,
            sessionStore = SessionStore(InMemorySessionPersistence()),
        )
        val navigation = NavigationStore(NavigationState(listOf(dashboard)))

        val result = assertIs<SduiActionResult.Navigate>(
            executor.execute(SduiAction.Navigate(booking, SduiNavigationMode.PUSH), emptyMap()),
        )
        navigation.dispatch(NavigationCommand.Push(result.destination))

        assertEquals("booking_details", (navigation.state.value.current as DynamicDestination).screenId)
        assertEquals("/api/v1/screens/booking/1", (navigation.state.value.current as DynamicDestination).endpoint)

        navigation.dispatch(NavigationCommand.Pop)
        assertEquals(dashboard, navigation.state.value.current)
    }

    private fun request(endpoint: String, mode: SduiNavigationMode): SduiAction.Request =
        SduiAction.Request(
            RequestPayload(
                method = SduiRequestMethod.POST,
                endpoint = endpoint,
                authentication = SduiAuthentication.NONE,
                responseMode = SduiRequestResponseMode.DESTINATION,
                navigationMode = mode,
            ),
        )

    private fun destinationJson(
        screenId: String,
        templateId: String,
        templateType: String,
        endpoint: String,
        authentication: String,
    ): JsonObject = JsonObject(
        mapOf(
            "screenId" to JsonPrimitive(screenId),
            "templateId" to JsonPrimitive(templateId),
            "templateType" to JsonPrimitive(templateType),
            "endpoint" to JsonPrimitive(endpoint),
            "method" to JsonPrimitive("GET"),
            "authentication" to JsonPrimitive(authentication),
        ),
    )

    private fun response(
        nextScreen: JsonObject,
        extraData: Map<String, kotlinx.serialization.json.JsonElement>,
    ): NetworkResult = NetworkResult.Success(
        NetworkResponse(
            statusCode = 200,
            body = JsonObject(
                mapOf(
                    "data" to JsonObject(extraData + ("nextScreen" to nextScreen)),
                ),
            ),
        ),
    )

    private class QueueNetworkDataSource(
        private val results: MutableList<NetworkResult>,
    ) : NetworkDataSource {
        override suspend fun execute(request: NetworkRequest): NetworkResult = results.removeAt(0)
    }

    private class InMemorySessionPersistence : SessionPersistence {
        var session: SessionState.Authenticated? = null

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
