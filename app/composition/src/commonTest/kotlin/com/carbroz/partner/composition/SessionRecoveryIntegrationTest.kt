package com.carbroz.partner.composition

import com.carbroz.data.network.NetworkAuthentication
import com.carbroz.data.network.NetworkEndpoint
import com.carbroz.data.network.NetworkEnvironment
import com.carbroz.data.network.NetworkExecutor
import com.carbroz.data.network.NetworkFailure
import com.carbroz.data.network.NetworkMethod
import com.carbroz.data.network.NetworkRequest
import com.carbroz.data.network.NetworkResult
import com.carbroz.data.network.NetworkResponse
import com.carbroz.data.network.NetworkTransport
import com.carbroz.data.network.SessionNetworkAuthenticationRecovery
import com.carbroz.data.network.SessionNetworkAuthorizationProvider
import com.carbroz.foundation.security.Secret
import com.carbroz.foundation.session.AuthTokens
import com.carbroz.foundation.session.SessionPersistence
import com.carbroz.foundation.session.SessionPersistenceResult
import com.carbroz.foundation.session.SessionRefreshCoordinator
import com.carbroz.foundation.session.SessionRestoreResult
import com.carbroz.foundation.session.SessionState
import com.carbroz.foundation.session.SessionStore
import com.carbroz.foundation.session.SessionSubject
import com.carbroz.foundation.session.SingleFlightTokenRefresher
import com.carbroz.foundation.session.TokenExpiryPolicy
import com.carbroz.foundation.time.Clock
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class SessionRecoveryIntegrationTest {
    @Test
    fun unauthorizedSessionRequestRefreshesSessionAndReplaysWithNewAccessToken() = runTest {
        val clock = Clock { 1_000L }
        val store = SessionStore(InMemorySessionPersistence())
        val oldTokens = tokens("old-access", "refresh-token", expiresAt = 2_000L)
        store.authenticate(authenticated(oldTokens))

        var protectedCalls = 0
        var refreshCalls = 0
        val seenAuthorization = mutableListOf<String?>()
        val transport = NetworkTransport { request ->
            when {
                request.url.endsWith("/api/v1/auth/refresh") -> {
                    refreshCalls += 1
                    NetworkResult.Success(
                        NetworkResponse(
                            statusCode = 200,
                            body = buildJsonObject {
                                put("accessToken", "new-access")
                                put("refreshToken", "new-refresh")
                                put("expiresInSeconds", 60L)
                            },
                        ),
                    )
                }

                else -> {
                    protectedCalls += 1
                    val authorization = request.headers["Authorization"]
                    seenAuthorization += authorization
                    if (authorization == "Bearer old-access") {
                        NetworkResult.Failure(NetworkFailure.Http(401))
                    } else {
                        NetworkResult.Success(NetworkResponse(200))
                    }
                }
            }
        }

        val delegate = CarBrozTokenRefresher(
            environment = NetworkEnvironment("https://api.carbroz.example"),
            transport = transport,
            clock = clock,
        )
        val coordinator = SessionRefreshCoordinator(
            sessionStore = store,
            expiryPolicy = TokenExpiryPolicy(clock),
            tokenRefresher = SingleFlightTokenRefresher(delegate, this),
        )
        val executor = NetworkExecutor(
            environment = NetworkEnvironment("https://api.carbroz.example"),
            transport = transport,
            authorizationProvider = SessionNetworkAuthorizationProvider(store),
            authenticationRecovery = SessionNetworkAuthenticationRecovery(coordinator),
        )

        val result = executor.execute(
            NetworkRequest(
                method = NetworkMethod.GET,
                endpoint = NetworkEndpoint("/protected"),
                authentication = NetworkAuthentication.SESSION,
            ),
        )

        assertIs<NetworkResult.Success>(result)
        assertEquals(2, protectedCalls)
        assertEquals(1, refreshCalls)
        assertEquals(listOf("Bearer old-access", "Bearer new-access"), seenAuthorization)

        val current = assertIs<SessionState.Authenticated>(store.current())
        assertEquals("new-access", current.tokens.accessToken.reveal())
        assertEquals("new-refresh", current.tokens.refreshToken?.reveal())
        assertEquals(61_000L, current.tokens.accessTokenExpiresAtEpochMilliseconds)
    }

    @Test
    fun rejectedRefreshFailsClosedAndLeavesExistingSessionUntouched() = runTest {
        val clock = Clock { 1_000L }
        val store = SessionStore(InMemorySessionPersistence())
        val oldTokens = tokens("old-access", "refresh-token", expiresAt = 2_000L)
        store.authenticate(authenticated(oldTokens))

        var protectedCalls = 0
        var refreshCalls = 0
        val transport = NetworkTransport { request ->
            if (request.url.endsWith("/api/v1/auth/refresh")) {
                refreshCalls += 1
                NetworkResult.Failure(NetworkFailure.Http(401))
            } else {
                protectedCalls += 1
                NetworkResult.Failure(NetworkFailure.Http(401))
            }
        }
        val coordinator = SessionRefreshCoordinator(
            sessionStore = store,
            expiryPolicy = TokenExpiryPolicy(clock),
            tokenRefresher = SingleFlightTokenRefresher(
                CarBrozTokenRefresher(
                    environment = NetworkEnvironment("https://api.carbroz.example"),
                    transport = transport,
                    clock = clock,
                ),
                this,
            ),
        )
        val executor = NetworkExecutor(
            environment = NetworkEnvironment("https://api.carbroz.example"),
            transport = transport,
            authorizationProvider = SessionNetworkAuthorizationProvider(store),
            authenticationRecovery = SessionNetworkAuthenticationRecovery(coordinator),
        )

        val failure = assertIs<NetworkResult.Failure>(
            executor.execute(
                NetworkRequest(
                    method = NetworkMethod.GET,
                    endpoint = NetworkEndpoint("/protected"),
                    authentication = NetworkAuthentication.SESSION,
                ),
            ),
        )

        assertEquals(NetworkFailure.Http(401), failure.error)
        assertEquals(1, protectedCalls)
        assertEquals(1, refreshCalls)
        val current = assertIs<SessionState.Authenticated>(store.current())
        assertEquals(oldTokens, current.tokens)
    }

    private fun tokens(access: String, refresh: String, expiresAt: Long) = AuthTokens(
        accessToken = Secret.of(access),
        refreshToken = Secret.of(refresh),
        accessTokenExpiresAtEpochMilliseconds = expiresAt,
    )

    private fun authenticated(tokens: AuthTokens) = SessionState.Authenticated(
        subject = SessionSubject("partner-1"),
        tokens = tokens,
    )

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
