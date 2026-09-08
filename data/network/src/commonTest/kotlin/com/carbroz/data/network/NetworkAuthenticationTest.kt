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
    fun optionalSessionExecutesWithOrWithoutRestoredAuthorization() = runTest {
        val captured = mutableListOf<TransportRequest>()
        val authenticated = executor(
            transport = NetworkTransport { captured += it; NetworkResult.Success(NetworkResponse(200)) },
            authorizationProvider = NetworkAuthorizationProvider { "Bearer restored" },
        )
        assertIs<NetworkResult.Success>(authenticated.execute(optionalSessionGet()))
        assertEquals("Bearer restored", captured.single().headers["Authorization"])

        captured.clear()
        val signedOut = executor(
            transport = NetworkTransport { captured += it; NetworkResult.Success(NetworkResponse(200)) },
            authorizationProvider = NetworkAuthorizationProvider { null },
        )
        assertIs<NetworkResult.Success>(signedOut.execute(optionalSessionGet()))
        assertNull(captured.single().headers["Authorization"])
    }

    @Test
    fun sessionRequestWithoutAuthenticatedAuthorizationFailsBeforeTransport() = runTest {
        var attempts = 0
        val executor = executor(
            transport = NetworkTransport { attempts += 1; NetworkResult.Success(NetworkResponse(200)) },
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
                if (attempts == 1) NetworkResult.Failure(NetworkFailure.Http(401))
                else NetworkResult.Success(NetworkResponse(200))
            },
            authorizationProvider = NetworkAuthorizationProvider { "Bearer $token" },
            authenticationRecovery = NetworkAuthenticationRecovery {
                recoveries += 1
                token = "new-token"
                NetworkAuthenticationRecoveryResult.Recovered
            },
        )
        assertIs<NetworkResult.Success>(executor.execute(sessionGet()))
        assertEquals(2, attempts)
        assertEquals(1, recoveries)
        assertEquals<List<String?>>(listOf("Bearer old-token", "Bearer new-token"), seenAuthorization)
    }

    @Test
    fun optionalSessionDoesNotReplayAsGuestAfterCanonicalSessionInvalidation() = runTest {
        var token: String? = "old-token"
        var attempts = 0
        var recoveries = 0
        val seenAuthorization = mutableListOf<String?>()
        val executor = executor(
            transport = NetworkTransport { request ->
                attempts += 1
                seenAuthorization += request.headers["Authorization"]
                NetworkResult.Failure(NetworkFailure.Http(401))
            },
            authorizationProvider = NetworkAuthorizationProvider { token?.let { "Bearer $it" } },
            authenticationRecovery = NetworkAuthenticationRecovery {
                recoveries += 1
                token = null
                NetworkAuthenticationRecoveryResult.SessionInvalidated
            },
        )

        val failure = assertIs<NetworkResult.Failure>(executor.execute(optionalSessionGet()))
        assertEquals(NetworkFailure.Http(401), failure.error)
        assertEquals(1, attempts)
        assertEquals(1, recoveries)
        assertEquals<List<String?>>(listOf("Bearer old-token"), seenAuthorization)
    }

    @Test
    fun requiredSessionDoesNotReplayAsGuestAfterSessionInvalidation() = runTest {
        var attempts = 0
        val executor = executor(
            transport = NetworkTransport { attempts += 1; NetworkResult.Failure(NetworkFailure.Http(401)) },
            authorizationProvider = NetworkAuthorizationProvider { "Bearer access-token" },
            authenticationRecovery = NetworkAuthenticationRecovery {
                NetworkAuthenticationRecoveryResult.SessionInvalidated
            },
        )

        val failure = assertIs<NetworkResult.Failure>(executor.execute(sessionGet()))
        assertEquals(NetworkFailure.Http(401), failure.error)
        assertEquals(1, attempts)
    }

    @Test
    fun unavailableRecoveryReturnsOriginalUnauthorizedWithoutReplay() = runTest {
        var attempts = 0
        var recoveries = 0
        val executor = executor(
            transport = NetworkTransport { attempts += 1; NetworkResult.Failure(NetworkFailure.Http(401)) },
            authorizationProvider = NetworkAuthorizationProvider { "Bearer access-token" },
            authenticationRecovery = NetworkAuthenticationRecovery {
                recoveries += 1
                NetworkAuthenticationRecoveryResult.Unavailable
            },
        )
        val failure = assertIs<NetworkResult.Failure>(executor.execute(sessionGet()))
        assertEquals(NetworkFailure.Http(401), failure.error)
        assertEquals(1, attempts)
        assertEquals(1, recoveries)
    }

    @Test
    fun secondUnauthorizedIsReturnedWithoutSecondRecovery() = runTest {
        var attempts = 0
        var recoveries = 0
        val executor = executor(
            transport = NetworkTransport { attempts += 1; NetworkResult.Failure(NetworkFailure.Http(401)) },
            authorizationProvider = NetworkAuthorizationProvider { "Bearer access-token" },
            authenticationRecovery = NetworkAuthenticationRecovery {
                recoveries += 1
                NetworkAuthenticationRecoveryResult.Recovered
            },
        )
        val failure = assertIs<NetworkResult.Failure>(executor.execute(sessionGet()))
        assertEquals(NetworkFailure.Http(401), failure.error)
        assertEquals(2, attempts)
        assertEquals(1, recoveries)
    }

    @Test
    fun mutationWithoutIdempotencyKeyDoesNotReplayAfterUnauthorized() = runTest {
        var attempts = 0
        var recoveries = 0
        val executor = executor(
            transport = NetworkTransport { attempts += 1; NetworkResult.Failure(NetworkFailure.Http(401)) },
            authorizationProvider = NetworkAuthorizationProvider { "Bearer access-token" },
            authenticationRecovery = NetworkAuthenticationRecovery {
                recoveries += 1
                NetworkAuthenticationRecoveryResult.Recovered
            },
        )
        val result = executor.execute(
            NetworkRequest(NetworkMethod.POST, NetworkEndpoint("/mutation"), authentication = NetworkAuthentication.SESSION),
        )
        val failure = assertIs<NetworkResult.Failure>(result)
        assertEquals(NetworkFailure.Http(401), failure.error)
        assertEquals(1, attempts)
        assertEquals(0, recoveries)
    }

    @Test
    fun concurrentUnauthorizedRequestsRemainSafeAroundRecoveryBoundary() = runTest {
        var token = "old-token"
        var recoveryCalls = 0
        val executor = executor(
            transport = NetworkTransport { request ->
                if (request.headers["Authorization"] == "Bearer old-token") NetworkResult.Failure(NetworkFailure.Http(401))
                else NetworkResult.Success(NetworkResponse(200))
            },
            authorizationProvider = NetworkAuthorizationProvider { "Bearer $token" },
            authenticationRecovery = NetworkAuthenticationRecovery {
                recoveryCalls += 1
                token = "new-token"
                NetworkAuthenticationRecoveryResult.Recovered
            },
        )
        val results = List(8) { async { executor.execute(sessionGet()) } }.awaitAll()
        results.forEach { assertIs<NetworkResult.Success>(it) }
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
        NetworkMethod.GET,
        NetworkEndpoint("/profile"),
        authentication = NetworkAuthentication.SESSION,
    )

    private fun optionalSessionGet() = NetworkRequest(
        NetworkMethod.GET,
        NetworkEndpoint("/bootstrap"),
        authentication = NetworkAuthentication.OPTIONAL_SESSION,
    )

    private fun publicGet() = NetworkRequest(NetworkMethod.GET, NetworkEndpoint("/public"))
}
