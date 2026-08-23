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
        when (val result = persistence.save(session)) {
            SessionPersistenceResult.Success -> {
                state = session
                SessionTransitionResult.Success(state)
            }

            is SessionPersistenceResult.Failed ->
                SessionTransitionResult.Failed(SessionTransitionFailure.Persistence(result.reason))
        }
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
}

sealed interface SessionTransitionResult {
    data class Success(val state: SessionState) : SessionTransitionResult
    data class Failed(val reason: SessionTransitionFailure) : SessionTransitionResult
}

sealed interface SessionTransitionFailure {
    data class Persistence(val reason: SessionPersistenceFailure) : SessionTransitionFailure
    data class RestoreRejected(val reason: SessionRestoreFailure) : SessionTransitionFailure
}
