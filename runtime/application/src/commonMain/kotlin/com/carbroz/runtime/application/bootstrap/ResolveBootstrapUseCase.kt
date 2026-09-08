package com.carbroz.runtime.application.bootstrap

import com.carbroz.foundation.session.SessionProvider
import com.carbroz.foundation.session.SessionState
import com.carbroz.runtime.application.startup.StartupBlocker
import com.carbroz.runtime.application.startup.StartupFailure
import com.carbroz.runtime.application.startup.StartupNotice
import com.carbroz.runtime.application.startup.StartupResolution

/** Resolves the current server bootstrap snapshot into one trusted application-startup outcome. */
class ResolveBootstrapUseCase(
    private val repository: BootstrapRepository,
    private val sessionProvider: SessionProvider,
    private val payloadDecoder: StartupPayloadDecoder,
) {
    suspend operator fun invoke(): ResolveBootstrapResult = when (val result = repository.load()) {
        is BootstrapRepositoryResult.Failure -> ResolveBootstrapResult.Failed(result.reason.toStartupFailure())
        is BootstrapRepositoryResult.Success -> resolve(result.snapshot)
    }

    private suspend fun resolve(snapshot: BootstrapSnapshot): ResolveBootstrapResult {
        val locallyAuthenticated = sessionProvider.current() is SessionState.Authenticated
        if (snapshot.authenticated != locallyAuthenticated) {
            return ResolveBootstrapResult.Failed(
                StartupFailure.Expected("bootstrap_session_mismatch", recoverable = false),
            )
        }

        val update = snapshot.update
        if (update.required) {
            val updateUri = update.updateUri?.takeIf(String::isNotBlank)
                ?: return ResolveBootstrapResult.Failed(
                    StartupFailure.Expected(
                        code = "bootstrap_required_update_missing_store_url",
                        recoverable = false,
                    ),
                )

            return ResolveBootstrapResult.Resolved(
                StartupResolution.Blocked(
                    StartupBlocker.RequiredUpdate(
                        title = "Update CarBroz Partner",
                        message = "A newer version of CarBroz Partner is required to continue.",
                        updateUri = updateUri,
                    ),
                ),
            )
        }

        val maintenance = snapshot.maintenance
        if (maintenance.enabled) {
            return ResolveBootstrapResult.Resolved(
                StartupResolution.Blocked(
                    StartupBlocker.Maintenance(
                        title = maintenance.title,
                        message = maintenance.message,
                        retryable = true,
                    ),
                ),
            )
        }

        val decoded = when (val result = payloadDecoder.decode(snapshot.nextPayload)) {
            is StartupPayloadDecodeResult.Success -> result.payload
            is StartupPayloadDecodeResult.Failure -> return ResolveBootstrapResult.Failed(
                StartupFailure.Expected(
                    code = "bootstrap_${result.code}",
                    recoverable = false,
                ),
            )
        }

        val notices = if (update.optional) {
            listOf(
                StartupNotice.OptionalUpdate(
                    latestVersion = update.latestVersion,
                    updateUri = update.updateUri?.takeIf(String::isNotBlank),
                ),
            )
        } else {
            emptyList()
        }

        return ResolveBootstrapResult.Resolved(
            StartupResolution.Ready(
                payload = decoded,
                notices = notices,
            ),
        )
    }

    private fun BootstrapRepositoryFailure.toStartupFailure(): StartupFailure.Expected = when (this) {
        BootstrapRepositoryFailure.Offline -> StartupFailure.Expected("bootstrap_offline", recoverable = true)
        BootstrapRepositoryFailure.Timeout -> StartupFailure.Expected("bootstrap_timeout", recoverable = true)
        BootstrapRepositoryFailure.Transport -> StartupFailure.Expected("bootstrap_transport", recoverable = true)
        is BootstrapRepositoryFailure.Http -> StartupFailure.Expected(
            code = "bootstrap_http_$statusCode",
            recoverable = statusCode == 401 || statusCode == 408 || statusCode == 429 || statusCode in 500..599,
        )
        is BootstrapRepositoryFailure.InvalidPayload -> StartupFailure.Expected(code, recoverable = false)
    }
}
