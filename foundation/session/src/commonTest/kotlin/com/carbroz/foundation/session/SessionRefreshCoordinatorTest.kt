package com.carbroz.foundation.session

import com.carbroz.foundation.security.Secret
import com.carbroz.foundation.time.Clock
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs

class SessionRefreshCoordinatorTest {
    @Test
    fun signedOutSessionDoesNotRefresh() = runTest {
        var refreshCalls = 0
        val store = SessionStore(FakeSessionPersistence())
        val coordinator = coordinator(store) {
            refreshCalls += 1
            TokenRefreshResult.Success(tokens("unused", expiresAt = 9_999L))
        }

        assertEquals(SessionRefreshResult.NotAuthenticated, coordinator.refreshIfNeeded())
        assertEquals(0, refreshCalls)
    }

    @Test
    fun validTokensDoNotRefresh() = runTest {
        var refreshCalls = 0
        val store = authenticatedStore(tokens("current", expiresAt = 100_000L))
        val coordinator = coordinator(store, now = 1_000L) {
            refreshCalls += 1
            TokenRefreshResult.Success(tokens("unused", expiresAt = 200_000L))
        }

        assertEquals(SessionRefreshResult.NotRequired, coordinator.refreshIfNeeded())
        assertEquals(0, refreshCalls)
    }

    @Test
    fun unknownExpiryDoesNotGuessRefreshPolicy() = runTest {
        var refreshCalls = 0
        val store = authenticatedStore(tokens("current", expiresAt = null))
        val coordinator = coordinator(store) {
            refreshCalls += 1
            TokenRefreshResult.Success(tokens("unused", expiresAt = 200_000L))
        }

        assertEquals(SessionRefreshResult.ExpiryUnknown, coordinator.refreshIfNeeded())
        assertEquals(0, refreshCalls)
    }

    @Test
    fun refreshRequiredWithoutRefreshTokenFailsBeforeDelegateCall() = runTest {
        var refreshCalls = 0
        val current = tokens("current", expiresAt = 1_000L, includeRefreshToken = false)
        val store = authenticatedStore(current)
        val coordinator = coordinator(store, now = 1_000L) {
            refreshCalls += 1
            TokenRefreshResult.Success(tokens("unused", expiresAt = 200_000L))
        }

        val result = assertIs<SessionRefreshResult.Failed>(coordinator.refreshIfNeeded())
        assertEquals(TokenRefreshFailure.MissingRefreshToken, result.reason)
        assertEquals(0, refreshCalls)
        assertEquals(current, assertIs<SessionState.Authenticated>(store.current()).tokens)
    }

    @Test
    fun successfulRefreshIsPersistedThroughSessionStore() = runTest {
        val current = tokens("current", expiresAt = 1_000L)
        val refreshed = tokens("refreshed", expiresAt = 100_000L)
        val persistence = FakeSessionPersistence()
        val store = authenticatedStore(current, persistence)
        val coordinator = coordinator(store, now = 1_000L) {
            TokenRefreshResult.Success(refreshed)
        }

        val result = assertIs<SessionRefreshResult.Refreshed>(coordinator.refreshIfNeeded())

        assertEquals(refreshed, result.tokens)
        assertEquals(refreshed, assertIs<SessionState.Authenticated>(store.current()).tokens)
        assertEquals(refreshed, persistence.lastSaved?.tokens)
    }

    @Test
    fun staleRefreshResultCannotOverwriteNewerSessionTokens() = runTest {
        val current = tokens("current", expiresAt = 1_000L)
        val newer = tokens("newer", expiresAt = 100_000L)
        val stale = tokens("stale", expiresAt = 100_000L)
        val persistence = FakeSessionPersistence()
        val store = authenticatedStore(current, persistence)
        val coordinator = coordinator(store, now = 1_000L) {
            store.updateTokens(current, newer)
            TokenRefreshResult.Success(stale)
        }

        val result = assertIs<SessionRefreshResult.ApplyFailed>(coordinator.refreshIfNeeded())

        assertEquals(SessionTransitionFailure.StaleSession, result.reason)
        assertEquals(newer, assertIs<SessionState.Authenticated>(store.current()).tokens)
    }

    @Test
    fun delegateFailureIsReturnedWithoutApplyingTokens() = runTest {
        val current = tokens("current", expiresAt = 1_000L)
        val store = authenticatedStore(current)
        val coordinator = coordinator(store, now = 1_000L) {
            TokenRefreshResult.Failed(TokenRefreshFailure.Rejected("expired-refresh"))
        }

        val result = assertIs<SessionRefreshResult.Failed>(coordinator.refreshIfNeeded())

        assertEquals(TokenRefreshFailure.Rejected("expired-refresh"), result.reason)
        assertEquals(current, assertIs<SessionState.Authenticated>(store.current()).tokens)
    }

    @Test
    fun cancellationFromDelegatePropagates() = runTest {
        val store = authenticatedStore(tokens("current", expiresAt = 1_000L))
        val coordinator = coordinator(store, now = 1_000L) {
            throw CancellationException("cancelled")
        }

        assertFailsWith<CancellationException> { coordinator.refreshIfNeeded() }
    }

    @Test
    fun unexpectedDelegateThrowableBecomesTypedFailure() = runTest {
        val current = tokens("current", expiresAt = 1_000L)
        val store = authenticatedStore(current)
        val coordinator = coordinator(store, now = 1_000L) {
            error("boom")
        }

        val result = assertIs<SessionRefreshResult.Failed>(coordinator.refreshIfNeeded())
        val failure = assertIs<TokenRefreshFailure.Unexpected>(result.reason)
        assertEquals("boom", failure.reason)
        assertEquals(current, assertIs<SessionState.Authenticated>(store.current()).tokens)
    }

    private suspend fun authenticatedStore(
        tokens: AuthTokens,
        persistence: FakeSessionPersistence = FakeSessionPersistence(),
    ): SessionStore = SessionStore(persistence).also {
        it.authenticate(SessionState.Authenticated(SessionSubject("subject"), tokens))
    }

    private fun coordinator(
        store: SessionStore,
        now: Long = 10_000L,
        refresher: suspend (AuthTokens) -> TokenRefreshResult,
    ) = SessionRefreshCoordinator(
        sessionStore = store,
        expiryPolicy = TokenExpiryPolicy(FixedClock(now)),
        tokenRefresher = TokenRefresher { refresher(it) },
    )

    private fun tokens(
        suffix: String,
        expiresAt: Long?,
        includeRefreshToken: Boolean = true,
    ) = AuthTokens(
        accessToken = Secret.of("access-$suffix"),
        refreshToken = if (includeRefreshToken) Secret.of("refresh-$suffix") else null,
        accessTokenExpiresAtEpochMilliseconds = expiresAt,
    )

    private class FixedClock(private val now: Long) : Clock {
        override fun nowEpochMilliseconds(): Long = now
    }

    private class FakeSessionPersistence : SessionPersistence {
        var lastSaved: SessionState.Authenticated? = null
        override suspend fun restore(): SessionRestoreResult = SessionRestoreResult.NoSession
        override suspend fun save(session: SessionState.Authenticated): SessionPersistenceResult {
            lastSaved = session
            return SessionPersistenceResult.Success
        }
        override suspend fun clear(): SessionPersistenceResult = SessionPersistenceResult.Success
    }
}
