package com.carbroz.partner.composition

import com.carbroz.foundation.session.SessionPersistence
import com.carbroz.foundation.session.SessionPersistenceResult
import com.carbroz.foundation.session.SessionRestoreFailure
import com.carbroz.foundation.session.SessionRestoreResult
import com.carbroz.foundation.session.SessionState
import com.carbroz.foundation.session.SessionStore
import com.carbroz.runtime.application.startup.StartupFailure
import com.carbroz.runtime.application.startup.StartupTaskResult
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class SessionRestoreStartupTaskTest {
    @Test
    fun clearsMalformedPersistedSnapshotAndContinuesSignedOut() = runTest {
        val persistence = FakePersistence(
            restoreResult = SessionRestoreResult.Rejected(SessionRestoreFailure.MalformedSnapshot),
        )
        val store = SessionStore(persistence)

        val result = SessionRestoreStartupTask(store).execute()

        assertEquals(StartupTaskResult.Success, result)
        assertEquals(1, persistence.clearCalls)
        assertEquals(SessionState.SignedOut, store.current())
    }

    @Test
    fun reportsStorageUnavailableAsRecoverableStartupFailure() = runTest {
        val store = SessionStore(
            FakePersistence(
                restoreResult = SessionRestoreResult.Rejected(SessionRestoreFailure.StorageUnavailable),
            ),
        )

        val result = assertIs<StartupTaskResult.Failure>(SessionRestoreStartupTask(store).execute())
        val failure = assertIs<StartupFailure.Expected>(result.reason)

        assertEquals("session_restore_storage_unavailable", failure.code)
        assertEquals(true, failure.recoverable)
    }

    private class FakePersistence(
        private val restoreResult: SessionRestoreResult,
    ) : SessionPersistence {
        var clearCalls: Int = 0

        override suspend fun restore(): SessionRestoreResult = restoreResult

        override suspend fun save(session: SessionState.Authenticated): SessionPersistenceResult =
            SessionPersistenceResult.Success

        override suspend fun clear(): SessionPersistenceResult {
            clearCalls += 1
            return SessionPersistenceResult.Success
        }
    }
}
