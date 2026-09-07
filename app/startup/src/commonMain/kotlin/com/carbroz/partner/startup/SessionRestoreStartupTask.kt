package com.carbroz.partner.startup

import com.carbroz.foundation.session.SessionRestoreFailure
import com.carbroz.foundation.session.SessionStore
import com.carbroz.foundation.session.SessionTransitionFailure
import com.carbroz.foundation.session.SessionTransitionResult
import com.carbroz.runtime.application.startup.StartupFailure
import com.carbroz.runtime.application.startup.StartupTask
import com.carbroz.runtime.application.startup.StartupTaskResult

/** Restores the canonical authenticated session before Partner bootstrap consumes it. */
class SessionRestoreStartupTask(
    private val sessionStore: SessionStore,
) : StartupTask {
    override val id: String = "session.restore"

    override suspend fun execute(): StartupTaskResult = when (val result = sessionStore.restore()) {
        is SessionTransitionResult.Success -> StartupTaskResult.Continue
        is SessionTransitionResult.Failed -> handleFailure(result.reason)
    }

    private suspend fun handleFailure(failure: SessionTransitionFailure): StartupTaskResult = when (failure) {
        is SessionTransitionFailure.RestoreRejected -> when (failure.reason) {
            SessionRestoreFailure.MalformedSnapshot,
            SessionRestoreFailure.UnsupportedSnapshotVersion -> recoverCorruptSnapshot()

            SessionRestoreFailure.StorageUnavailable -> StartupTaskResult.Failure(
                StartupFailure.Expected("session_restore_storage_unavailable", recoverable = true),
            )
        }

        is SessionTransitionFailure.Persistence -> StartupTaskResult.Failure(
            StartupFailure.Expected("session_restore_persistence_failure", recoverable = true),
        )

        SessionTransitionFailure.NotAuthenticated,
        SessionTransitionFailure.StaleSession -> StartupTaskResult.Failure(StartupFailure.Unexpected)
    }

    private suspend fun recoverCorruptSnapshot(): StartupTaskResult = when (sessionStore.signOut()) {
        is SessionTransitionResult.Success -> StartupTaskResult.Continue
        is SessionTransitionResult.Failed -> StartupTaskResult.Failure(
            StartupFailure.Expected("session_restore_cleanup_failed", recoverable = true),
        )
    }
}
