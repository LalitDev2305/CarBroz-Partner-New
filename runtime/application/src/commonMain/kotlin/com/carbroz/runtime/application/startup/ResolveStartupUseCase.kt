package com.carbroz.runtime.application.startup

import com.carbroz.foundation.observability.CorrelationId
import com.carbroz.foundation.observability.CorrelationIdProvider
import com.carbroz.foundation.observability.CrashEvent
import com.carbroz.foundation.observability.DiagnosticAttribute
import com.carbroz.foundation.observability.NoOpObservability
import com.carbroz.foundation.observability.Observability
import com.carbroz.foundation.observability.PerformanceMetric
import com.carbroz.foundation.observability.RandomCorrelationIdProvider
import com.carbroz.foundation.observability.TraceOutcome
import com.carbroz.foundation.observability.TraceSpan
import com.carbroz.foundation.session.SessionStore
import com.carbroz.foundation.session.SessionTransitionResult
import com.carbroz.foundation.time.Clock
import com.carbroz.foundation.time.SystemClock
import kotlinx.coroutines.CancellationException

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
    private val observability: Observability = NoOpObservability,
    private val clock: Clock = SystemClock,
    private val correlationIdProvider: CorrelationIdProvider = RandomCorrelationIdProvider(),
) {
    suspend operator fun invoke(): StartupResult {
        val startedAt = clock.nowEpochMilliseconds()
        val correlationId = correlationIdProvider.next("startup")

        return try {
            val result = resolve()
            finish(result, startedAt, correlationId)
            result
        } catch (cancellation: CancellationException) {
            observability.trace(
                TraceSpan(
                    name = STARTUP_TRACE,
                    correlationId = correlationId,
                    durationMillis = elapsedSince(startedAt),
                    outcome = TraceOutcome.CANCELLED,
                ),
            )
            throw cancellation
        } catch (error: Throwable) {
            val result = StartupResult.Failure(
                code = "startup_unexpected_failure",
                recoverable = false,
            )
            observability.crash(
                CrashEvent(
                    category = "startup",
                    message = "Unexpected application startup failure.",
                    correlationId = correlationId,
                ),
                error,
            )
            finish(result, startedAt, correlationId)
            result
        }
    }

    private suspend fun resolve(): StartupResult {
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

    private fun finish(
        result: StartupResult,
        startedAt: Long,
        correlationId: CorrelationId,
    ) {
        val outcome = if (result is StartupResult.Failure) TraceOutcome.FAILURE else TraceOutcome.SUCCESS
        val attributes = mapOf("outcome" to DiagnosticAttribute(result.metricOutcome()))
        val duration = elapsedSince(startedAt)
        observability.performance(
            PerformanceMetric(
                name = STARTUP_TRACE,
                durationMillis = duration,
                correlationId = correlationId,
                attributes = attributes,
            ),
        )
        observability.trace(
            TraceSpan(
                name = STARTUP_TRACE,
                correlationId = correlationId,
                durationMillis = duration,
                outcome = outcome,
                attributes = attributes,
            ),
        )
    }

    private fun StartupResult.metricOutcome(): String = when (this) {
        is StartupResult.Ready -> "ready"
        is StartupResult.RequiredUpdate -> "required_update"
        is StartupResult.Maintenance -> "maintenance"
        is StartupResult.Failure -> "failure"
    }

    private fun elapsedSince(startedAt: Long): Long =
        (clock.nowEpochMilliseconds() - startedAt).coerceAtLeast(0L)

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

    private companion object {
        const val STARTUP_TRACE = "application.startup"
    }
}
