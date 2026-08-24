package com.carbroz.data.network

import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs

class NetworkBoundaryTest {
    @Test
    fun networkRequestUsesRelativeEndpoint() {
        val request = request()

        assertEquals("/auth/send-otp", request.endpoint.value)
        assertEquals(NetworkMethod.POST, request.method)
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
            transport = NetworkTransport { transportRequest ->
                captured = transportRequest
                NetworkResult.Success(NetworkResponse(statusCode = 200))
            },
            headerProvider = NetworkHeaderProvider { mapOf("Authorization" to "Bearer token") },
        )

        val result = executor.execute(request(mapOf("X-Request-Id" to "123")))
        val transportRequest = requireNotNull(captured)

        assertIs<NetworkResult.Success>(result)
        assertEquals("https://api.carbroz.example/auth/send-otp", transportRequest.url)
        assertEquals("Bearer token", transportRequest.headers["Authorization"])
        assertEquals("123", transportRequest.headers["X-Request-Id"])
        assertEquals("POST", transportRequest.method)
    }

    @Test
    fun requestCannotOverrideAuthorization() = runTest {
        val executor = NetworkExecutor(
            environment = NetworkEnvironment("https://api.carbroz.example"),
            transport = NetworkTransport { NetworkResult.Success(NetworkResponse(200)) },
            headerProvider = NetworkHeaderProvider { mapOf("Authorization" to "trusted") },
        )

        val result = executor.execute(
            request(mapOf("authorization" to "untrusted")),
        )

        val failure = assertIs<NetworkResult.Failure>(result)
        assertIs<NetworkFailure.InvalidRequest>(failure.error)
    }

    private fun request(headers: Map<String, String> = emptyMap()) = NetworkRequest(
        method = NetworkMethod.POST,
        endpoint = NetworkEndpoint("/auth/send-otp"),
        payload = JsonObject(mapOf("phone" to JsonPrimitive("9876543210"))),
        headers = headers,
    )
}
