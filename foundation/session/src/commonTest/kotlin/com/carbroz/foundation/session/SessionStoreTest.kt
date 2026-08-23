package com.carbroz.foundation.session

import com.carbroz.foundation.security.Secret
import kotlinx.coroutines.async
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class SessionStoreTest {
    @Test
    fun startsSignedOut() = runTest {
        val store = SessionStore(FakeSessionPersistence())

        assertEquals(SessionState.SignedOut, store.current())
    }

    @Test
    fun restoreNoSessionKeepsSignedOut() = runTest {
        val store = SessionStore(FakeSessionPersistence(restoreResult = SessionRestoreResult.NoSession))

        val result = store.restore()

        assertEquals(SessionState.SignedOut, assertIs<SessionTransitionResult.Success>(result).state)
        assertEquals(SessionState.SignedOut, store.current())
    }

    @Test
    fun restoreAuthenticatedSessionBecomesCurrent() = runTest {
        val session = authenticated("subject-a")
        val store = SessionStore(
            FakeSessionPersistence(restoreResult = SessionRestoreResult.Restored(session)),
        )

        val result = store.restore()

        assertEquals(session, assertIs<SessionTransitionResult.Success>(result).state)
        assertEquals(session, store.current())
    }

    @Test
    fun rejectedRestoreDoesNotReplaceExistingState() = runTest {
        val session = authenticated("subject-a")
        val persistence = FakeSessionPersistence()
        val store = SessionStore(persistence)
        store.authenticate(session)
        persistence.restoreResult = SessionRestoreResult.Rejected(SessionRestoreFailure.MalformedSnapshot)

        val result = store.restore()

        assertEquals(
            SessionTransitionFailure.RestoreRejected(SessionRestoreFailure.MalformedSnapshot),
            assertIs<SessionTransitionResult.Failed>(result).reason,
        )
        assertEquals(session, store.current())
    }

    @Test
    fun authenticatePublishesStateOnlyAfterPersistenceSucceeds() = runTest {
        val session = authenticated("subject-a")
        val persistence = FakeSessionPersistence(saveResult = SessionPersistenceResult.Success)
        val store = SessionStore(persistence)

        val result = store.authenticate(session)

        assertEquals(session, assertIs<SessionTransitionResult.Success>(result).state)
        assertEquals(session, store.current())
        assertEquals(session, persistence.lastSaved)
    }

    @Test
    fun failedAuthenticatePreservesPreviousState() = runTest {
        val first = authenticated("subject-a")
        val second = authenticated("subject-b")
        val persistence = FakeSessionPersistence()
        val store = SessionStore(persistence)
        store.authenticate(first)
        persistence.saveResult = SessionPersistenceResult.Failed(SessionPersistenceFailure.StorageUnavailable)

        val result = store.authenticate(second)

        assertEquals(
            SessionTransitionFailure.Persistence(SessionPersistenceFailure.StorageUnavailable),
            assertIs<SessionTransitionResult.Failed>(result).reason,
        )
        assertEquals(first, store.current())
    }

    @Test
    fun signOutClearsPersistenceBeforePublishingSignedOut() = runTest {
        val session = authenticated("subject-a")
        val persistence = FakeSessionPersistence()
        val store = SessionStore(persistence)
        store.authenticate(session)

        val result = store.signOut()

        assertEquals(SessionState.SignedOut, assertIs<SessionTransitionResult.Success>(result).state)
        assertEquals(SessionState.SignedOut, store.current())
        assertEquals(1, persistence.clearCalls)
    }

    @Test
    fun failedSignOutPreservesAuthenticatedState() = runTest {
        val session = authenticated("subject-a")
        val persistence = FakeSessionPersistence()
        val store = SessionStore(persistence)
        store.authenticate(session)
        persistence.clearResult = SessionPersistenceResult.Failed(SessionPersistenceFailure.StorageUnavailable)

        val result = store.signOut()

        assertIs<SessionTransitionResult.Failed>(result)
        assertEquals(session, store.current())
    }

    @Test
    fun concurrentMutationsAreSerialized() = runTest {
        val persistence = FakeSessionPersistence()
        val store = SessionStore(persistence)
        val first = authenticated("subject-a")
        val second = authenticated("subject-b")

        val firstResult = async { store.authenticate(first) }
        val secondResult = async { store.authenticate(second) }

        firstResult.await()
        secondResult.await()

        assertEquals(second, store.current())
        assertEquals(listOf(first, second), persistence.savedSessions)
    }

    private fun authenticated(subject: String) = SessionState.Authenticated(
        subject = SessionSubject(subject),
        tokens = AuthTokens(
            accessToken = Secret.of("access-$subject"),
            refreshToken = Secret.of("refresh-$subject"),
            accessTokenExpiresAtEpochMilliseconds = 123_456L,
        ),
    )

    private class FakeSessionPersistence(
        var restoreResult: SessionRestoreResult = SessionRestoreResult.NoSession,
        var saveResult: SessionPersistenceResult = SessionPersistenceResult.Success,
        var clearResult: SessionPersistenceResult = SessionPersistenceResult.Success,
    ) : SessionPersistence {
        var lastSaved: SessionState.Authenticated? = null
        val savedSessions = mutableListOf<SessionState.Authenticated>()
        var clearCalls = 0

        override suspend fun restore(): SessionRestoreResult = restoreResult

        override suspend fun save(session: SessionState.Authenticated): SessionPersistenceResult {
            lastSaved = session
            savedSessions += session
            return saveResult
        }

        override suspend fun clear(): SessionPersistenceResult {
            clearCalls += 1
            return clearResult
        }
    }
}
