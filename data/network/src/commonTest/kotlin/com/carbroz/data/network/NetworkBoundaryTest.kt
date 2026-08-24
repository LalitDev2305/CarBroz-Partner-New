package com.carbroz.data.network

import com.carbroz.runtime.action.PreparedAction
import com.carbroz.runtime.sdui.model.NodeType
import com.carbroz.runtime.sdui.model.RequestMethod
import com.carbroz.runtime.sdui.model.ScreenDestination
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs

class NetworkBoundaryTest {
    @Test
    fun preparedActionBecomesRelativeTrustedRequest() {
        val request = prepared().toNetworkRequest()

        assertEquals("/auth/send-otp", request.endpoint.value)
        assertEquals("otp", request.destination.screenId)
    }

    @Test
    fun endpointRejectsAbsoluteAndProtocolRelativeUrls() {
        assertFailsWith<IllegalArgumentException> { NetworkEndpoint("https://evil.example/send") }
        assertFailsWith<IllegalArgumentException> { NetworkEndpoint("//evil.example/send") }
    }

    @Test
    fun executorOwnsBaseUrlAndTransportHeaders() = runTest {
        var captured: TransportRequest? = null
        val executor = NetworkExecutor(
            environment = NetworkEnvironment("https://api.carbroz.example"),
            transport = NetworkTransport { request ->
                captured = request
                NetworkResult.Success(NetworkResponse(statusCode = 200))
            },
            headerProvider = NetworkHeaderProvider { mapOf("Authorization" to "Bearer token") },
        )

        val result = executor.execute(prepared().toNetworkRequest(mapOf("X-Request-Id" to "123")))
        val request = requireNotNull(captured)

        assertIs<NetworkResult.Success>(result)
        assertEquals("https://api.carbroz.example/auth/send-otp", request.url)
        assertEquals("Bearer token", request.headers["Authorization"])
        assertEquals("123", request.headers["X-Request-Id"])
        assertEquals("POST", request.method)
    }

    @Test
    fun requestCannotOverrideAuthorization() = runTest {
        val executor = NetworkExecutor(
            environment = NetworkEnvironment("https://api.carbroz.example"),
            transport = NetworkTransport { NetworkResult.Success(NetworkResponse(200)) },
            headerProvider = NetworkHeaderProvider { mapOf("Authorization" to "trusted") },
        )

        val result = executor.execute(
            prepared().toNetworkRequest(mapOf("authorization" to "untrusted")),
        )

        val failure = assertIs<NetworkResult.Failure>(result)
        assertIs<NetworkFailure.InvalidRequest>(failure.error)
    }

    private fun prepared() = PreparedAction.Request(
        method = RequestMethod.POST,
        endpoint = "/auth/send-otp",
        destination = ScreenDestination(
            screenId = "otp",
            templateId = "auth_otp",
            templateType = NodeType("FORM_TEMPLATE"),
        ),
        payload = JsonObject(mapOf("phone" to JsonPrimitive("9876543210"))),
    )
}
