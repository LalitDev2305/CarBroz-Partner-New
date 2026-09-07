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

/** Executes startup tasks sequentially until one task resolves the final outcome or fails. */
class StartupCoordinator(
    private val tasks: List<StartupTask>,
    private val observability: Observability = NoOpObservability,
    private val clock: Clock = SystemClock,
    private val correlationIdProvider: CorrelationIdProvider = CorrelationIdProvider {
        CorrelationId("startup-untracked")
    },
) {
    init {
        require(tasks.isNotEmpty()) { "Startup requires at least one task." }
        require(tasks.map(StartupTask::id).distinct().size == tasks.size) {
            "Startup task ids must be unique."
        }
    }

    suspend fun run(): StartupResult {
        val startupStartedAt = clock.nowEpochMilliseconds()
        val correlationId = correlationIdProvider.next("startup")
        observability.log(LogEvent(LogLevel.INFO, CATEGORY, "startup_started", correlationId))

        tasks.forEach { task ->
            val taskStartedAt = clock.nowEpochMilliseconds()
            observability.log(
                LogEvent(
                    level = LogLevel.DEBUG,
                    category = CATEGORY,
                    message = "startup_task_started",
                    correlationId = correlationId,
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
                    CrashEvent(
                        category = CATEGORY,
                        message = "startup_task_unexpected_failure",
                        correlationId = correlationId,
                        attributes = mapOf("task_id" to DiagnosticAttribute(task.id)),
                    ),
                    failure,
                )
                recordTaskCompletion(task.id, taskStartedAt, correlationId, "unexpected_failure", TraceOutcome.FAILURE)
                recordStartupTrace(startupStartedAt, correlationId, TraceOutcome.FAILURE)
                return StartupResult.Failed(task.id, StartupFailure.Unexpected)
            }

            when (result) {
                StartupTaskResult.Continue -> recordTaskCompletion(
                    task.id,
                    taskStartedAt,
                    correlationId,
                    "continue",
                    TraceOutcome.SUCCESS,
                )

                is StartupTaskResult.Resolved -> {
                    recordTaskCompletion(task.id, taskStartedAt, correlationId, "resolved", TraceOutcome.SUCCESS)
                    recordResolved(startupStartedAt, correlationId, result.resolution)
                    return StartupResult.Resolved(result.resolution)
                }

                is StartupTaskResult.Failure -> {
                    recordTaskCompletion(task.id, taskStartedAt, correlationId, "failure", TraceOutcome.FAILURE)
                    observability.log(
                        LogEvent(
                            level = LogLevel.WARN,
                            category = CATEGORY,
                            message = "startup_task_failed",
                            correlationId = correlationId,
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

        observability.log(
            LogEvent(
                level = LogLevel.ERROR,
                category = CATEGORY,
                message = "startup_missing_resolution",
                correlationId = correlationId,
            ),
        )
        recordStartupTrace(startupStartedAt, correlationId, TraceOutcome.FAILURE)
        return StartupResult.Failed(
            taskId = COORDINATOR_TASK_ID,
            failure = StartupFailure.Expected("startup_missing_resolution", recoverable = false),
        )
    }

    private fun recordResolved(
        startedAt: Long,
        correlationId: CorrelationId,
        resolution: StartupResolution,
    ) {
        val totalDuration = elapsedSince(startedAt)
        val message = when (resolution) {
            is StartupResolution.Ready -> "startup_ready"
            is StartupResolution.Blocked -> "startup_blocked"
        }
        observability.performance(PerformanceMetric("startup.total", totalDuration, correlationId))
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
                message = message,
                correlationId = correlationId,
            ),
        )
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
        observability.performance(PerformanceMetric("startup.task", duration, correlationId, attributes))
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
        const val COORDINATOR_TASK_ID = "startup.coordinator"
    }
}

sealed interface StartupResult {
    data class Resolved(val resolution: StartupResolution) : StartupResult

    data class Failed(
        val taskId: String,
        val failure: StartupFailure,
    ) : StartupResult
}
