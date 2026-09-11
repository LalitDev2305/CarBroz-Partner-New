package com.carbroz.runtime.application.startup

import com.carbroz.foundation.session.SessionStore
import com.carbroz.foundation.session.SessionTransitionResult

/**
 * Single application orchestrator for Partner startup.
 *
 * Session restore remains owned by foundation:session and HTTP adaptation remains owned by
 * data:bootstrap. This use case only sequences canonical owners and applies startup policy.
 */
class ResolveStartupUseCase(
    private val sessionStore: SessionStore,
    private val bootstrapRepository: BootstrapRepository,
    private val partnerConfigStore: PartnerConfigStore,
) {
    suspend operator fun invoke(): StartupResult {
        when (sessionStore.restore()) {
            is SessionTransitionResult.Success -> Unit
            is SessionTransitionResult.Failed -> {
                return StartupResult.Failure(
                    code = "session_restore_failed",
                    recoverable = true,
                )
            }
        }

        val snapshot = when (val result = bootstrapRepository.load()) {
            is BootstrapRepositoryResult.Success -> result.snapshot
            is BootstrapRepositoryResult.Failure -> return result.reason.toStartupFailure()
        }

        partnerConfigStore.update(snapshot.config)

        val update = snapshot.config.update
        if (update.required) {
            val updateUri = update.updateUri?.takeIf(String::isNotBlank)
                ?: return StartupResult.Failure(
                    code = "bootstrap_required_update_missing_store_url",
                    recoverable = false,
                )
            return StartupResult.RequiredUpdate(
                title = "Update CarBroz Partner",
                message = "A newer version of CarBroz Partner is required to continue.",
                updateUri = updateUri,
            )
        }

        val maintenance = snapshot.maintenance
        if (maintenance.enabled) {
            return StartupResult.Maintenance(
                title = maintenance.title,
                message = maintenance.message,
                retryable = true,
            )
        }

        return StartupResult.Ready(snapshot.nextScreen)
    }

    private fun BootstrapRepositoryFailure.toStartupFailure(): StartupResult.Failure = when (this) {
        BootstrapRepositoryFailure.Offline -> StartupResult.Failure("bootstrap_offline", recoverable = true)
        BootstrapRepositoryFailure.Timeout -> StartupResult.Failure("bootstrap_timeout", recoverable = true)
        BootstrapRepositoryFailure.Transport -> StartupResult.Failure("bootstrap_transport", recoverable = true)
        is BootstrapRepositoryFailure.Http -> StartupResult.Failure(
            code = "bootstrap_http_$statusCode",
            recoverable = statusCode == 401 || statusCode == 408 || statusCode == 429 || statusCode in 500..599,
        )
        is BootstrapRepositoryFailure.InvalidPayload -> StartupResult.Failure(code, recoverable = false)
    }
}
