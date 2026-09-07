package com.carbroz.partner.startup

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

class SessionRestoreStartupTaskTest {
    @Test
    fun `no persisted session continues startup as signed out`() = runTest {
        val persistence = FakePersistence(SessionRestoreResult.NoSession)
        val task = SessionRestoreStartupTask(SessionStore(persistence))

        assertEquals(StartupTaskResult.Continue, task.execute())
    }

    @Test
    fun `malformed persisted session is cleared and startup continues`() = runTest {
        val persistence = FakePersistence(
            SessionRestoreResult.Rejected(SessionRestoreFailure.MalformedSnapshot),
        )
        val task = SessionRestoreStartupTask(SessionStore(persistence))

        assertEquals(StartupTaskResult.Continue, task.execute())
        assertEquals(1, persistence.clearCalls)
    }

    @Test
    fun `storage failure remains recoverable startup failure`() = runTest {
        val persistence = FakePersistence(
            SessionRestoreResult.Rejected(SessionRestoreFailure.StorageUnavailable),
        )
        val task = SessionRestoreStartupTask(SessionStore(persistence))

        assertEquals(
            StartupTaskResult.Failure(
                StartupFailure.Expected("session_restore_storage_unavailable", recoverable = true),
            ),
            task.execute(),
        )
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
