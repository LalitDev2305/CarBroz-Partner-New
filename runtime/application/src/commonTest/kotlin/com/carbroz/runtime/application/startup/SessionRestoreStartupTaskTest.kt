package com.carbroz.runtime.application.startup

import com.carbroz.foundation.session.SessionPersistence
import com.carbroz.foundation.session.SessionPersistenceResult
import com.carbroz.foundation.session.SessionRestoreFailure
import com.carbroz.foundation.session.SessionRestoreResult
import com.carbroz.foundation.session.SessionState
import com.carbroz.foundation.session.SessionStore
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class SessionRestoreStartupTaskTest {
    @Test
    fun `no persisted session continues startup`() = runTest {
        val task = SessionRestoreStartupTask(SessionStore(FakePersistence(SessionRestoreResult.NoSession)))
        assertEquals(StartupTaskResult.Continue, task.execute())
    }

    @Test
    fun `invalid persisted session is repaired by session owner and continues`() = runTest {
        val persistence = FakePersistence(
            SessionRestoreResult.Rejected(SessionRestoreFailure.MalformedSnapshot),
        )
        val task = SessionRestoreStartupTask(SessionStore(persistence))

        assertEquals(StartupTaskResult.Continue, task.execute())
        assertEquals(1, persistence.clearCalls)
    }

    @Test
    fun `storage failure remains recoverable`() = runTest {
        val task = SessionRestoreStartupTask(
            SessionStore(
                FakePersistence(SessionRestoreResult.Rejected(SessionRestoreFailure.StorageUnavailable)),
            ),
        )

        assertEquals(
            StartupTaskResult.Failure(
                StartupFailure.Expected("session_restore_storage_unavailable", recoverable = true),
            ),
            task.execute(),
        )
    }

    @Test
    fun `cleanup persistence failure remains recoverable`() = runTest {
        val persistence = FakePersistence(
            restoreResult = SessionRestoreResult.Rejected(SessionRestoreFailure.UnsupportedSnapshotVersion),
            clearResult = SessionPersistenceResult.Failed(
                com.carbroz.foundation.session.SessionPersistenceFailure.StorageUnavailable,
            ),
        )
        val task = SessionRestoreStartupTask(SessionStore(persistence))

        assertEquals(
            StartupTaskResult.Failure(
                StartupFailure.Expected("session_restore_persistence_failure", recoverable = true),
            ),
            task.execute(),
        )
    }

    private class FakePersistence(
        private val restoreResult: SessionRestoreResult,
        private val clearResult: SessionPersistenceResult = SessionPersistenceResult.Success,
    ) : SessionPersistence {
        var clearCalls: Int = 0

        override suspend fun restore(): SessionRestoreResult = restoreResult

        override suspend fun save(session: SessionState.Authenticated): SessionPersistenceResult =
            SessionPersistenceResult.Success

        override suspend fun clear(): SessionPersistenceResult {
            clearCalls += 1
            return clearResult
        }
    }
}
