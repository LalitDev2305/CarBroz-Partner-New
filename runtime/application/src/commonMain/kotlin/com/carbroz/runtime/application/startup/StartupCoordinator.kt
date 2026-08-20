package com.carbroz.runtime.application.startup

/**
 * Executes startup tasks sequentially in their declared order.
 *
 * Startup stops at the first controlled failure. This makes initialization
 * ordering deterministic and prevents later tasks from running against missing
 * prerequisites.
 */
class StartupCoordinator(
    private val tasks: List<StartupTask>,
) {
    suspend fun run(): StartupResult {
        tasks.forEach { task ->
            when (val result = task.execute()) {
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
