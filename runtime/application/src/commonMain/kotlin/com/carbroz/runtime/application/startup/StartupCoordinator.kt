package com.carbroz.runtime.application.startup

import com.carbroz.foundation.observability.CrashEvent
import com.carbroz.foundation.observability.DiagnosticAttribute
import com.carbroz.foundation.observability.LogEvent
import com.carbroz.foundation.observability.LogLevel
import com.carbroz.foundation.observability.NoOpObservability
import com.carbroz.foundation.observability.Observability
import com.carbroz.foundation.observability.PerformanceMetric
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
) {
    init {
        require(tasks.map(StartupTask::id).distinct().size == tasks.size) {
            "Startup task ids must be unique."
        }
    }

    suspend fun run(): StartupResult {
        val startupStartedAt = clock.nowEpochMilliseconds()
        observability.log(LogEvent(LogLevel.INFO, CATEGORY, "startup_started"))

        tasks.forEach { task ->
            val taskStartedAt = clock.nowEpochMilliseconds()
            observability.log(
                LogEvent(
                    level = LogLevel.DEBUG,
                    category = CATEGORY,
                    message = "startup_task_started",
                    attributes = mapOf("task_id" to DiagnosticAttribute(task.id)),
                ),
            )

            val result = try {
                task.execute()
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (failure: Throwable) {
                observability.crash(
                    event = CrashEvent(
                        category = CATEGORY,
                        message = "startup_task_unexpected_failure",
                        attributes = mapOf("task_id" to DiagnosticAttribute(task.id)),
                    ),
                    throwable = failure,
                )
                recordTaskDuration(task.id, taskStartedAt, "unexpected_failure")
                return StartupResult.Failed(task.id, StartupFailure.Unexpected)
            }

            when (result) {
                StartupTaskResult.Success -> recordTaskDuration(task.id, taskStartedAt, "success")
                is StartupTaskResult.Failure -> {
                    recordTaskDuration(task.id, taskStartedAt, "failure")
                    observability.log(
                        LogEvent(
                            level = LogLevel.WARN,
                            category = CATEGORY,
                            message = "startup_task_failed",
                            attributes = mapOf(
                                "task_id" to DiagnosticAttribute(task.id),
                                "recoverable" to DiagnosticAttribute(result.reason.recoverable.toString()),
                            ),
                        ),
                    )
                    return StartupResult.Failed(task.id, result.reason)
                }
            }
        }

        observability.performance(
            PerformanceMetric(
                name = "startup.total",
                durationMillis = elapsedSince(startupStartedAt),
            ),
        )
        observability.log(LogEvent(LogLevel.INFO, CATEGORY, "startup_ready"))
        return StartupResult.Ready
    }

    private fun recordTaskDuration(taskId: String, startedAt: Long, outcome: String) {
        observability.performance(
            PerformanceMetric(
                name = "startup.task",
                durationMillis = elapsedSince(startedAt),
                attributes = mapOf(
                    "task_id" to DiagnosticAttribute(taskId),
                    "outcome" to DiagnosticAttribute(outcome),
                ),
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
