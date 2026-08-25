package com.carbroz.runtime.application.startup

import com.carbroz.foundation.observability.CorrelationId
import com.carbroz.foundation.observability.CorrelationIdProvider
import com.carbroz.foundation.observability.CrashEvent
import com.carbroz.foundation.observability.DiagnosticAttribute
import com.carbroz.foundation.observability.LogEvent
import com.carbroz.foundation.observability.LogLevel
import com.carbroz.foundation.observability.NoOpObservability
import com.carbroz.foundation.observability.Observability
import com.carbroz.foundation.observability.PerformanceMetric
import com.carbroz.foundation.observability.TraceOutcome
import com.carbroz.foundation.observability.TraceSpan
import com.carbroz.foundation.time.Clock
import com.carbroz.foundation.time.SystemClock
import kotlinx.coroutines.CancellationException

/**
 * Executes startup tasks sequentially in their declared order.
 *
 * Startup stops at the first failure. Expected failures are returned by tasks;
 * unexpected task exceptions are converted into a stable runtime failure while
 * coroutine cancellation is always propagated to the caller.
 *
 * Task identifiers must be unique so diagnostics and retry policy address one
 * unambiguous bootstrap step.
 */
class StartupCoordinator(
    private val tasks: List<StartupTask>,
    private val observability: Observability = NoOpObservability,
    private val clock: Clock = SystemClock,
    private val correlationIdProvider: CorrelationIdProvider = CorrelationIdProvider {
        CorrelationId("startup-untracked")
    },
) {
    init {
        require(tasks.map(StartupTask::id).distinct().size == tasks.size) {
            "Startup task ids must be unique."
        }
    }

    suspend fun run(): StartupResult {
        val startupStartedAt = clock.nowEpochMilliseconds()
        val correlationId = correlationIdProvider.next("startup")
        observability.log(
            LogEvent(
                level = LogLevel.INFO,
                category = CATEGORY,
                message = "startup_started",
                correlationId = correlationId.value,
            ),
        )

        tasks.forEach { task ->
            val taskStartedAt = clock.nowEpochMilliseconds()
            observability.log(
                LogEvent(
                    level = LogLevel.DEBUG,
                    category = CATEGORY,
                    message = "startup_task_started",
                    correlationId = correlationId.value,
                    attributes = mapOf("task_id" to DiagnosticAttribute(task.id)),
                ),
            )

            val result = try {
                task.execute()
            } catch (cancellation: CancellationException) {
                recordTaskCompletion(task.id, taskStartedAt, correlationId, "cancelled", TraceOutcome.CANCELLED)
                recordStartupTrace(startupStartedAt, correlationId, TraceOutcome.CANCELLED)
                throw cancellation
            } catch (failure: Throwable) {
                observability.crash(
                    event = CrashEvent(
                        category = CATEGORY,
                        message = "startup_task_unexpected_failure",
                        correlationId = correlationId.value,
                        attributes = mapOf("task_id" to DiagnosticAttribute(task.id)),
                    ),
                    throwable = failure,
                )
                recordTaskCompletion(task.id, taskStartedAt, correlationId, "unexpected_failure", TraceOutcome.FAILURE)
                recordStartupTrace(startupStartedAt, correlationId, TraceOutcome.FAILURE)
                return StartupResult.Failed(task.id, StartupFailure.Unexpected)
            }

            when (result) {
                StartupTaskResult.Success -> recordTaskCompletion(
                    task.id,
                    taskStartedAt,
                    correlationId,
                    "success",
                    TraceOutcome.SUCCESS,
                )
                is StartupTaskResult.Failure -> {
                    recordTaskCompletion(task.id, taskStartedAt, correlationId, "failure", TraceOutcome.FAILURE)
                    observability.log(
                        LogEvent(
                            level = LogLevel.WARN,
                            category = CATEGORY,
                            message = "startup_task_failed",
                            correlationId = correlationId.value,
                            attributes = mapOf(
                                "task_id" to DiagnosticAttribute(task.id),
                                "recoverable" to DiagnosticAttribute(result.reason.recoverable.toString()),
                            ),
                        ),
                    )
                    recordStartupTrace(startupStartedAt, correlationId, TraceOutcome.FAILURE)
                    return StartupResult.Failed(task.id, result.reason)
                }
            }
        }

        val totalDuration = elapsedSince(startupStartedAt)
        observability.performance(
            PerformanceMetric(
                name = "startup.total",
                durationMillis = totalDuration,
                correlationId = correlationId.value,
            ),
        )
        observability.trace(
            TraceSpan(
                name = "startup.total",
                correlationId = correlationId,
                durationMillis = totalDuration,
                outcome = TraceOutcome.SUCCESS,
            ),
        )
        observability.log(
            LogEvent(
                level = LogLevel.INFO,
                category = CATEGORY,
                message = "startup_ready",
                correlationId = correlationId.value,
            ),
        )
        return StartupResult.Ready
    }

    private fun recordTaskCompletion(
        taskId: String,
        startedAt: Long,
        correlationId: CorrelationId,
        outcome: String,
        traceOutcome: TraceOutcome,
    ) {
        val duration = elapsedSince(startedAt)
        val attributes = mapOf(
            "task_id" to DiagnosticAttribute(taskId),
            "outcome" to DiagnosticAttribute(outcome),
        )
        observability.performance(
            PerformanceMetric(
                name = "startup.task",
                durationMillis = duration,
                correlationId = correlationId.value,
                attributes = attributes,
            ),
        )
        observability.trace(
            TraceSpan(
                name = "startup.task",
                correlationId = correlationId,
                durationMillis = duration,
                outcome = traceOutcome,
                attributes = attributes,
            ),
        )
    }

    private fun recordStartupTrace(startedAt: Long, correlationId: CorrelationId, outcome: TraceOutcome) {
        observability.trace(
            TraceSpan(
                name = "startup.total",
                correlationId = correlationId,
                durationMillis = elapsedSince(startedAt),
                outcome = outcome,
            ),
        )
    }

    private fun elapsedSince(startedAt: Long): Long =
        (clock.nowEpochMilliseconds() - startedAt).coerceAtLeast(0L)

    private companion object {
        const val CATEGORY = "startup"
    }
}

/** Aggregate outcome of application startup. */
sealed interface StartupResult {
    data object Ready : StartupResult

    data class Failed(
        val taskId: String,
        val failure: StartupFailure,
    ) : StartupResult
}
