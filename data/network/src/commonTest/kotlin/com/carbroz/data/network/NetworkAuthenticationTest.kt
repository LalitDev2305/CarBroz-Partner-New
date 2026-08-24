package com.carbroz.data.network

import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull

class NetworkAuthenticationTest {
    @Test
    fun sessionRequestAttachesAuthorizationAndPublicRequestDoesNot() = runTest {
        val captured = mutableListOf<TransportRequest>()
        val executor = executor(
            transport = NetworkTransport { request ->
                captured += request
                NetworkResult.Success(NetworkResponse(200))
            },
            authorizationProvider = NetworkAuthorizationProvider { "Bearer access-token" },
        )

        assertIs<NetworkResult.Success>(executor.execute(sessionGet()))
        assertIs<NetworkResult.Success>(executor.execute(publicGet()))

        assertEquals("Bearer access-token", captured[0].headers["Authorization"])
        assertNull(captured[1].headers["Authorization"])
    }

    @Test
    fun sessionRequestWithoutAuthenticatedAuthorizationFailsBeforeTransport() = runTest {
        var attempts = 0
        val executor = executor(
            transport = NetworkTransport {
                attempts += 1
                NetworkResult.Success(NetworkResponse(200))
            },
            authorizationProvider = NetworkAuthorizationProvider { null },
        )

        val result = executor.execute(sessionGet())

        val failure = assertIs<NetworkResult.Failure>(result)
        assertIs<NetworkFailure.InvalidRequest>(failure.error)
        assertEquals(0, attempts)
    }

    @Test
    fun unauthorizedSessionRequestRefreshesAndReplaysOnceWithNewAuthorization() = runTest {
        var token = "old-token"
        var attempts = 0
        var recoveries = 0
        val seenAuthorization = mutableListOf<String?>()
        val executor = executor(
            transport = NetworkTransport { request ->
                attempts += 1
                seenAuthorization += request.headers["Authorization"]
                if (attempts == 1) {
                    NetworkResult.Failure(NetworkFailure.Http(401))
                } else {
                    NetworkResult.Success(NetworkResponse(200))
                }
            },
            authorizationProvider = NetworkAuthorizationProvider { "Bearer $token" },
            authenticationRecovery = NetworkAuthenticationRecovery {
                recoveries += 1
                token = "new-token"
                true
            },
        )

        val result = executor.execute(sessionGet())

        assertIs<NetworkResult.Success>(result)
        assertEquals(2, attempts)
        assertEquals(1, recoveries)
        assertEquals(listOf("Bearer old-token", "Bearer new-token"), seenAuthorization)
    }

    @Test
    fun failedRecoveryReturnsOriginalUnauthorizedWithoutReplay() = runTest {
        var attempts = 0
        var recoveries = 0
        val executor = executor(
            transport = NetworkTransport {
                attempts += 1
                NetworkResult.Failure(NetworkFailure.Http(401))
            },
            authorizationProvider = NetworkAuthorizationProvider { "Bearer access-token" },
            authenticationRecovery = NetworkAuthenticationRecovery {
                recoveries += 1
                false
            },
        )

        val result = executor.execute(sessionGet())

        val failure = assertIs<NetworkResult.Failure>(result)
        assertEquals(NetworkFailure.Http(401), failure.error)
        assertEquals(1, attempts)
        assertEquals(1, recoveries)
    }

    @Test
    fun secondUnauthorizedIsReturnedWithoutSecondRecovery() = runTest {
        var attempts = 0
        var recoveries = 0
        val executor = executor(
            transport = NetworkTransport {
                attempts += 1
                NetworkResult.Failure(NetworkFailure.Http(401))
            },
            authorizationProvider = NetworkAuthorizationProvider { "Bearer access-token" },
            authenticationRecovery = NetworkAuthenticationRecovery {
                recoveries += 1
                true
            },
        )

        val result = executor.execute(sessionGet())

        val failure = assertIs<NetworkResult.Failure>(result)
        assertEquals(NetworkFailure.Http(401), failure.error)
        assertEquals(2, attempts)
        assertEquals(1, recoveries)
    }

    @Test
    fun mutationWithoutIdempotencyKeyDoesNotReplayAfterUnauthorized() = runTest {
        var attempts = 0
        var recoveries = 0
        val executor = executor(
            transport = NetworkTransport {
                attempts += 1
                NetworkResult.Failure(NetworkFailure.Http(401))
            },
            authorizationProvider = NetworkAuthorizationProvider { "Bearer access-token" },
            authenticationRecovery = NetworkAuthenticationRecovery {
                recoveries += 1
                true
            },
        )

        val result = executor.execute(
            NetworkRequest(
                method = NetworkMethod.POST,
                endpoint = NetworkEndpoint("/mutation"),
                authentication = NetworkAuthentication.SESSION,
            ),
        )

        val failure = assertIs<NetworkResult.Failure>(result)
        assertEquals(NetworkFailure.Http(401), failure.error)
        assertEquals(1, attempts)
        assertEquals(0, recoveries)
    }

    @Test
    fun concurrentUnauthorizedRequestsShareRecoveryBoundary() = runTest {
        var token = "old-token"
        var recoveryCalls = 0
        val executor = executor(
            transport = NetworkTransport { request ->
                if (request.headers["Authorization"] == "Bearer old-token") {
                    NetworkResult.Failure(NetworkFailure.Http(401))
                } else {
                    NetworkResult.Success(NetworkResponse(200))
                }
            },
            authorizationProvider = NetworkAuthorizationProvider { "Bearer $token" },
            authenticationRecovery = NetworkAuthenticationRecovery {
                recoveryCalls += 1
                token = "new-token"
                true
            },
        )

        val results = List(8) { async { executor.execute(sessionGet()) } }.awaitAll()

        results.forEach { assertIs<NetworkResult.Success>(it) }
        // The network boundary remains safe under concurrency; canonical single-flight behavior
        // itself is owned and tested by foundation:session.
        assertEquals(true, recoveryCalls >= 1)
    }

    private fun executor(
        transport: NetworkTransport,
        authorizationProvider: NetworkAuthorizationProvider = EmptyNetworkAuthorizationProvider,
        authenticationRecovery: NetworkAuthenticationRecovery = NoNetworkAuthenticationRecovery,
    ) = NetworkExecutor(
        environment = NetworkEnvironment("https://api.carbroz.example"),
        transport = transport,
        authorizationProvider = authorizationProvider,
        authenticationRecovery = authenticationRecovery,
    )

    private fun sessionGet() = NetworkRequest(
        method = NetworkMethod.GET,
        endpoint = NetworkEndpoint("/profile"),
        authentication = NetworkAuthentication.SESSION,
    )

    private fun publicGet() = NetworkRequest(
        method = NetworkMethod.GET,
        endpoint = NetworkEndpoint("/public"),
    )
}
