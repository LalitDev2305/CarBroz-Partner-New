package com.carbroz.runtime.application.startup

import com.carbroz.foundation.session.SessionRestoreFailure
import com.carbroz.foundation.session.SessionStore
import com.carbroz.foundation.session.SessionTransitionFailure
import com.carbroz.foundation.session.SessionTransitionResult

/** Thin startup adapter over the canonical session owner's complete restoration policy. */
class SessionRestoreStartupTask(
    private val sessionStore: SessionStore,
) : StartupTask {
    override val id: String = "session.restore"

    override suspend fun execute(): StartupTaskResult = when (val result = sessionStore.restore()) {
        is SessionTransitionResult.Success -> StartupTaskResult.Continue
        is SessionTransitionResult.Failed -> StartupTaskResult.Failure(result.reason.toStartupFailure())
    }

    private fun SessionTransitionFailure.toStartupFailure(): StartupFailure = when (this) {
        is SessionTransitionFailure.RestoreRejected -> when (reason) {
            SessionRestoreFailure.StorageUnavailable ->
                StartupFailure.Expected("session_restore_storage_unavailable", recoverable = true)

            SessionRestoreFailure.MalformedSnapshot,
            SessionRestoreFailure.UnsupportedSnapshotVersion,
            -> StartupFailure.Unexpected
        }

        is SessionTransitionFailure.Persistence ->
            StartupFailure.Expected("session_restore_persistence_failure", recoverable = true)

        SessionTransitionFailure.NotAuthenticated,
        SessionTransitionFailure.StaleSession,
        -> StartupFailure.Unexpected
    }
}
