package com.carbroz.foundation.session

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Canonical in-memory owner of the current authenticated session.
 *
 * Mutations are serialized. Persisted state is changed before the in-memory
 * state so callers never observe a successful transition that was not durably
 * recorded. Cancellation always propagates.
 */
class SessionStore(
    private val persistence: SessionPersistence,
) : SessionProvider {
    private val mutex = Mutex()
    private var state: SessionState = SessionState.SignedOut

    override suspend fun current(): SessionState = mutex.withLock { state }

    suspend fun restore(): SessionTransitionResult = mutex.withLock {
        when (val restored = persistence.restore()) {
            SessionRestoreResult.NoSession -> {
                state = SessionState.SignedOut
                SessionTransitionResult.Success(state)
            }

            is SessionRestoreResult.Restored -> {
                state = restored.session
                SessionTransitionResult.Success(state)
            }

            is SessionRestoreResult.Rejected ->
                SessionTransitionResult.Failed(SessionTransitionFailure.RestoreRejected(restored.reason))
        }
    }

    suspend fun authenticate(session: SessionState.Authenticated): SessionTransitionResult = mutex.withLock {
        persistAndPublish(session)
    }

    /**
     * Replaces only the token set of the currently authenticated session.
     *
     * This keeps [SessionStore] as the sole session state owner. Refresh
     * orchestration can obtain new credentials independently, but it must apply
     * them through this boundary so durable and in-memory state cannot diverge.
     */
    suspend fun updateTokens(tokens: AuthTokens): SessionTransitionResult = mutex.withLock {
        val authenticated = state as? SessionState.Authenticated
            ?: return@withLock SessionTransitionResult.Failed(SessionTransitionFailure.NotAuthenticated)

        persistAndPublish(authenticated.copy(tokens = tokens))
    }

    suspend fun signOut(): SessionTransitionResult = mutex.withLock {
        when (val result = persistence.clear()) {
            SessionPersistenceResult.Success -> {
                state = SessionState.SignedOut
                SessionTransitionResult.Success(state)
            }

            is SessionPersistenceResult.Failed ->
                SessionTransitionResult.Failed(SessionTransitionFailure.Persistence(result.reason))
        }
    }

    private suspend fun persistAndPublish(
        session: SessionState.Authenticated,
    ): SessionTransitionResult = when (val result = persistence.save(session)) {
        SessionPersistenceResult.Success -> {
            state = session
            SessionTransitionResult.Success(state)
        }

        is SessionPersistenceResult.Failed ->
            SessionTransitionResult.Failed(SessionTransitionFailure.Persistence(result.reason))
    }
}

sealed interface SessionTransitionResult {
    data class Success(val state: SessionState) : SessionTransitionResult
    data class Failed(val reason: SessionTransitionFailure) : SessionTransitionResult
}

sealed interface SessionTransitionFailure {
    data class Persistence(val reason: SessionPersistenceFailure) : SessionTransitionFailure
    data class RestoreRejected(val reason: SessionRestoreFailure) : SessionTransitionFailure
    data object NotAuthenticated : SessionTransitionFailure
}
