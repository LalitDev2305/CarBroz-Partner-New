package com.carbroz.runtime.application.startup

import kotlinx.coroutines.CancellationException

/**
 * Executes startup tasks sequentially in their declared order.
 *
 * Startup stops at the first failure. Expected failures are returned by tasks;
 * unexpected task exceptions are converted into a stable runtime failure while
 * coroutine cancellation is always propagated to the caller.
 *
 * Task identifiers must be unique so diagnostics, retry policy, and future
 * observability can address one unambiguous bootstrap step.
 */
class StartupCoordinator(
    private val tasks: List<StartupTask>,
) {
    init {
        require(tasks.map(StartupTask::id).distinct().size == tasks.size) {
            "Startup task ids must be unique."
        }
    }

    suspend fun run(): StartupResult {
        tasks.forEach { task ->
            val result = try {
                task.execute()
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (_: Throwable) {
                return StartupResult.Failed(
                    taskId = task.id,
                    failure = StartupFailure.Unexpected,
                )
            }

            when (result) {
                StartupTaskResult.Success -> Unit
                is StartupTaskResult.Failure -> return StartupResult.Failed(
                    taskId = task.id,
                    failure = result.reason,
                )
            }
        }
        return StartupResult.Ready
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
