package com.carbroz.data.network

import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class NetworkObservabilityTest {
    @Test
    fun successfulExecutionEmitsStartedAttemptAndFinishedWithStableContext() = runTest {
        val events = mutableListOf<NetworkObservation>()
        val executor = executor(
            transport = NetworkTransport {
                NetworkResult.Success(NetworkResponse(200))
            },
            events = events,
        )

        val result = executor.execute(publicGet())

        assertIs<NetworkResult.Success>(result)
        assertEquals(3, events.size)
        assertIs<NetworkObservation.Started>(events[0])
        assertIs<NetworkObservation.AttemptStarted>(events[1])
        val finished = assertIs<NetworkObservation.Finished>(events[2])
        assertEquals(NetworkOutcome.Success(200), finished.outcome)
        assertEquals(setOf(NetworkRequestId("request-123")), events.map { it.context.requestId }.toSet())
        assertEquals(NetworkMethod.GET, finished.context.method)
        assertEquals(NetworkEndpoint("/public"), finished.context.endpoint)
    }

    @Test
    fun retryUsesSameRequestContextAndEmitsScheduledDelay() = runTest {
        val events = mutableListOf<NetworkObservation>()
        var attempts = 0
        val executor = executor(
            transport = NetworkTransport {
                attempts += 1
                if (attempts == 1) {
                    NetworkResult.Failure(NetworkFailure.Transport)
                } else {
                    NetworkResult.Success(NetworkResponse(200))
                }
            },
            events = events,
            retryDelay = NetworkRetryDelay { },
        )

        val result = executor.execute(
            publicGet().copy(
                executionPolicy = NetworkExecutionPolicy(
                    maxAttempts = 2,
                    initialRetryDelayMillis = 125,
                ),
            ),
        )

        assertIs<NetworkResult.Success>(result)
        assertEquals(
            listOf(
                NetworkObservation.Started::class,
                NetworkObservation.AttemptStarted::class,
                NetworkObservation.RetryScheduled::class,
                NetworkObservation.AttemptStarted::class,
                NetworkObservation.Finished::class,
            ),
            events.map { it::class },
        )
        val retry = assertIs<NetworkObservation.RetryScheduled>(events[2])
        assertEquals(1, retry.attempt)
        assertEquals(125, retry.delayMillis)
        assertEquals(setOf(NetworkRequestId("request-123")), events.map { it.context.requestId }.toSet())
        assertEquals(NetworkOutcome.Success(200), assertIs<NetworkObservation.Finished>(events.last()).outcome)
    }

    @Test
    fun authenticationRecoveryReusesSameContextAcrossReplay() = runTest {
        val events = mutableListOf<NetworkObservation>()
        var token = "old-token"
        var calls = 0
        val executor = executor(
            transport = NetworkTransport { request ->
                calls += 1
                if (request.headers["Authorization"] == "Bearer old-token") {
                    NetworkResult.Failure(NetworkFailure.Http(401))
                } else {
                    NetworkResult.Success(NetworkResponse(200))
                }
            },
            events = events,
            authorizationProvider = NetworkAuthorizationProvider { "Bearer $token" },
            authenticationRecovery = NetworkAuthenticationRecovery {
                token = "new-token"
                true
            },
        )

        val result = executor.execute(
            publicGet().copy(authentication = NetworkAuthentication.SESSION),
        )

        assertIs<NetworkResult.Success>(result)
        assertEquals(2, calls)
        assertEquals(
            listOf(
                NetworkObservation.Started::class,
                NetworkObservation.AttemptStarted::class,
                NetworkObservation.AuthenticationRecoveryStarted::class,
                NetworkObservation.AuthenticationRecoveryFinished::class,
                NetworkObservation.AttemptStarted::class,
                NetworkObservation.Finished::class,
            ),
            events.map { it::class },
        )
        assertEquals(setOf(NetworkRequestId("request-123")), events.map { it.context.requestId }.toSet())
    }

    @Test
    fun failedAuthenticationRecoveryFinishesWithSanitizedUnauthorizedOutcome() = runTest {
        val events = mutableListOf<NetworkObservation>()
        val executor = executor(
            transport = NetworkTransport {
                NetworkResult.Failure(NetworkFailure.Http(401))
            },
            events = events,
            authorizationProvider = NetworkAuthorizationProvider { "Bearer secret-token" },
            authenticationRecovery = NetworkAuthenticationRecovery { false },
        )

        val result = executor.execute(
            publicGet().copy(authentication = NetworkAuthentication.SESSION),
        )

        assertIs<NetworkResult.Failure>(result)
        val finished = assertIs<NetworkObservation.Finished>(events.last())
        assertEquals(NetworkOutcome.HttpFailure(401), finished.outcome)
        assertEquals(false, assertIs<NetworkObservation.AuthenticationRecoveryFinished>(events[3]).recovered)
    }

    @Test
    fun invalidRequestEmitsStartedAndFinishedWithoutAttempt() = runTest {
        val events = mutableListOf<NetworkObservation>()
        val executor = executor(
            transport = NetworkTransport {
                error("transport must not be called")
            },
            events = events,
        )

        val result = executor.execute(
            NetworkRequest(
                method = NetworkMethod.POST,
                endpoint = NetworkEndpoint("/mutation"),
                executionPolicy = NetworkExecutionPolicy(maxAttempts = 2),
            ),
        )

        assertIs<NetworkResult.Failure>(result)
        assertEquals(2, events.size)
        assertIs<NetworkObservation.Started>(events[0])
        assertEquals(
            NetworkOutcome.InvalidRequest,
            assertIs<NetworkObservation.Finished>(events[1]).outcome,
        )
    }

    @Test
    fun observabilityContractContainsNoPayloadHeadersOrRawFailureReason() = runTest {
        val events = mutableListOf<NetworkObservation>()
        val executor = executor(
            transport = NetworkTransport {
                NetworkResult.Failure(NetworkFailure.Transport)
            },
            events = events,
        )

        executor.execute(
            NetworkRequest(
                method = NetworkMethod.POST,
                endpoint = NetworkEndpoint("/submit"),
                payload = kotlinx.serialization.json.JsonObject(
                    mapOf("secret" to kotlinx.serialization.json.JsonPrimitive("payload-value")),
                ),
                headers = mapOf("X-Custom" to "header-secret"),
            ),
        )

        val finished = assertIs<NetworkObservation.Finished>(events.last())
        assertEquals(NetworkOutcome.TransportFailure, finished.outcome)
        val rendered = events.joinToString("|")
        assertEquals(false, "payload-value" in rendered)
        assertEquals(false, "header-secret" in rendered)
    }

    private fun executor(
        transport: NetworkTransport,
        events: MutableList<NetworkObservation>,
        authorizationProvider: NetworkAuthorizationProvider = EmptyNetworkAuthorizationProvider,
        authenticationRecovery: NetworkAuthenticationRecovery = NoNetworkAuthenticationRecovery,
        retryDelay: NetworkRetryDelay = CoroutineNetworkRetryDelay,
    ) = NetworkExecutor(
        environment = NetworkEnvironment("https://api.carbroz.example"),
        transport = transport,
        authorizationProvider = authorizationProvider,
        authenticationRecovery = authenticationRecovery,
        retryDelay = retryDelay,
        requestIdProvider = NetworkRequestIdProvider { NetworkRequestId("request-123") },
        observer = NetworkObserver { events += it },
    )

    private fun publicGet() = NetworkRequest(
        method = NetworkMethod.GET,
        endpoint = NetworkEndpoint("/public"),
    )
}
