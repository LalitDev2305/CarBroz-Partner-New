package com.carbroz.data.network

import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertNull

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
    fun executorOwnsBaseUrlTransportHeadersAndJsonEncoding() = runTest {
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
        assertEquals("{\"phone\":\"9876543210\"}", transportRequest.body)
    }

    @Test
    fun bodylessRequestReachesTransportWithoutSyntheticBody() = runTest {
        var captured: TransportRequest? = null
        val executor = NetworkExecutor(
            environment = NetworkEnvironment("https://api.carbroz.example"),
            transport = NetworkTransport { transportRequest ->
                captured = transportRequest
                NetworkResult.Success(NetworkResponse(statusCode = 200))
            },
        )

        val result = executor.execute(
            NetworkRequest(
                method = NetworkMethod.GET,
                endpoint = NetworkEndpoint("/bootstrap"),
            ),
        )
        val transportRequest = requireNotNull(captured)

        assertIs<NetworkResult.Success>(result)
        assertEquals("GET", transportRequest.method)
        assertEquals("https://api.carbroz.example/bootstrap", transportRequest.url)
        assertNull(transportRequest.body)
    }

    @Test
    fun requestCannotOverrideAuthorizationOrIdempotencyHeader() = runTest {
        val executor = NetworkExecutor(
            environment = NetworkEnvironment("https://api.carbroz.example"),
            transport = NetworkTransport { NetworkResult.Success(NetworkResponse(200)) },
            headerProvider = NetworkHeaderProvider { mapOf("Authorization" to "trusted") },
        )

        listOf("authorization", "Idempotency-Key").forEach { forbiddenHeader ->
            val result = executor.execute(request(mapOf(forbiddenHeader to "untrusted")))
            val failure = assertIs<NetworkResult.Failure>(result)
            assertIs<NetworkFailure.InvalidRequest>(failure.error)
        }
    }

    @Test
    fun requestRejectsInvalidHeaderNamesAndValuesBeforeTransport() = runTest {
        var transportCalls = 0
        val executor = NetworkExecutor(
            environment = NetworkEnvironment("https://api.carbroz.example"),
            transport = NetworkTransport {
                transportCalls += 1
                NetworkResult.Success(NetworkResponse(200))
            },
        )

        val invalidHeaders = listOf(
            mapOf("" to "value"),
            mapOf("Bad Header" to "value"),
            mapOf("X-Test" to "safe\r\nInjected: value"),
            mapOf("X-Test" to "safe\u0000value"),
        )

        invalidHeaders.forEach { headers ->
            val result = executor.execute(request(headers))
            val failure = assertIs<NetworkResult.Failure>(result)
            assertIs<NetworkFailure.InvalidRequest>(failure.error)
        }
        assertEquals(0, transportCalls)
    }

    @Test
    fun providerRejectsInvalidHeaderNamesAndValuesBeforeTransport() = runTest {
        var transportCalls = 0
        val invalidProviderHeaders = listOf(
            mapOf("Bad Header" to "value"),
            mapOf("X-Provider" to "safe\nInjected: value"),
            mapOf("X-Provider" to "safe\u007Fvalue"),
        )

        invalidProviderHeaders.forEach { providerHeaders ->
            val executor = NetworkExecutor(
                environment = NetworkEnvironment("https://api.carbroz.example"),
                transport = NetworkTransport {
                    transportCalls += 1
                    NetworkResult.Success(NetworkResponse(200))
                },
                headerProvider = NetworkHeaderProvider { providerHeaders },
            )

            val result = executor.execute(request())
            val failure = assertIs<NetworkResult.Failure>(result)
            assertIs<NetworkFailure.InvalidRequest>(failure.error)
        }
        assertEquals(0, transportCalls)
    }

    @Test
    fun timeoutIsNormalizedAtExecutorBoundary() = runTest {
        val executor = NetworkExecutor(
            environment = NetworkEnvironment("https://api.carbroz.example"),
            transport = NetworkTransport {
                awaitCancellation()
            },
        )

        val result = executor.execute(
            NetworkRequest(
                method = NetworkMethod.GET,
                endpoint = NetworkEndpoint("/bootstrap"),
                executionPolicy = NetworkExecutionPolicy(timeoutMillis = 100),
            ),
        )

        val failure = assertIs<NetworkResult.Failure>(result)
        assertEquals(NetworkFailure.Timeout, failure.error)
    }

    @Test
    fun retryableFailureRetriesThenReturnsSuccess() = runTest {
        var attempts = 0
        val delays = mutableListOf<Long>()
        val executor = NetworkExecutor(
            environment = NetworkEnvironment("https://api.carbroz.example"),
            transport = NetworkTransport {
                attempts += 1
                if (attempts == 1) {
                    NetworkResult.Failure(NetworkFailure.Transport)
                } else {
                    NetworkResult.Success(NetworkResponse(200))
                }
            },
            retryDelay = NetworkRetryDelay { delays += it },
        )

        val result = executor.execute(
            NetworkRequest(
                method = NetworkMethod.GET,
                endpoint = NetworkEndpoint("/bootstrap"),
                executionPolicy = NetworkExecutionPolicy(
                    maxAttempts = 3,
                    initialRetryDelayMillis = 100,
                ),
            ),
        )

        assertIs<NetworkResult.Success>(result)
        assertEquals(2, attempts)
        assertEquals(listOf(100L), delays)
    }

    @Test
    fun nonRetryableHttpFailureDoesNotRetry() = runTest {
        var attempts = 0
        val executor = NetworkExecutor(
            environment = NetworkEnvironment("https://api.carbroz.example"),
            transport = NetworkTransport {
                attempts += 1
                NetworkResult.Failure(NetworkFailure.Http(400))
            },
            retryDelay = NetworkRetryDelay { error("delay must not run") },
        )

        val result = executor.execute(
            NetworkRequest(
                method = NetworkMethod.GET,
                endpoint = NetworkEndpoint("/bootstrap"),
                executionPolicy = NetworkExecutionPolicy(maxAttempts = 3),
            ),
        )

        val failure = assertIs<NetworkResult.Failure>(result)
        assertIs<NetworkFailure.Http>(failure.error)
        assertEquals(1, attempts)
    }

    @Test
    fun postRetryRequiresAndAppliesIdempotencyKey() = runTest {
        var attemptsWithoutKey = 0
        val executorWithoutKey = NetworkExecutor(
            environment = NetworkEnvironment("https://api.carbroz.example"),
            transport = NetworkTransport {
                attemptsWithoutKey += 1
                NetworkResult.Failure(NetworkFailure.Transport)
            },
            retryDelay = NetworkRetryDelay { },
        )

        val invalidResult = executorWithoutKey.execute(
            request().copy(executionPolicy = NetworkExecutionPolicy(maxAttempts = 2)),
        )

        val invalidFailure = assertIs<NetworkResult.Failure>(invalidResult)
        assertIs<NetworkFailure.InvalidRequest>(invalidFailure.error)
        assertEquals(0, attemptsWithoutKey)

        var attemptsWithKey = 0
        var capturedIdempotencyKey: String? = null
        val executorWithKey = NetworkExecutor(
            environment = NetworkEnvironment("https://api.carbroz.example"),
            transport = NetworkTransport { transportRequest ->
                attemptsWithKey += 1
                capturedIdempotencyKey = transportRequest.headers["Idempotency-Key"]
                if (attemptsWithKey == 1) {
                    NetworkResult.Failure(NetworkFailure.Http(503))
                } else {
                    NetworkResult.Success(NetworkResponse(200))
                }
            },
            retryDelay = NetworkRetryDelay { },
        )

        val successResult = executorWithKey.execute(
            request().copy(
                executionPolicy = NetworkExecutionPolicy(maxAttempts = 2),
                idempotencyKey = "send-otp-123",
            ),
        )

        assertIs<NetworkResult.Success>(successResult)
        assertEquals(2, attemptsWithKey)
        assertEquals("send-otp-123", capturedIdempotencyKey)
    }

    @Test
    fun retryBackoffIsBounded() {
        val policy = NetworkExecutionPolicy(
            maxAttempts = 5,
            initialRetryDelayMillis = 100,
            maxRetryDelayMillis = 250,
            backoffMultiplier = 2.0,
        )

        assertEquals(100, policy.retryDelayMillis(0))
        assertEquals(200, policy.retryDelayMillis(1))
        assertEquals(250, policy.retryDelayMillis(2))
        assertEquals(250, policy.retryDelayMillis(3))
    }

    private fun request(headers: Map<String, String> = emptyMap()) = NetworkRequest(
        method = NetworkMethod.POST,
        endpoint = NetworkEndpoint("/auth/send-otp"),
        payload = JsonObject(mapOf("phone" to JsonPrimitive("9876543210"))),
        headers = headers,
    )
}
