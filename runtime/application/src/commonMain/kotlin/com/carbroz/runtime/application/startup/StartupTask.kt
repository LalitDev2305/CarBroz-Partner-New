package com.carbroz.runtime.application.startup

/**
 * One ordered unit of application bootstrap work.
 *
 * Tasks must be product-neutral at the runtime layer and return a typed result
 * rather than leaking expected bootstrap failures as exceptions.
 */
interface StartupTask {
    val id: String
    suspend fun execute(): StartupTaskResult
}

/** Result of executing a single [StartupTask]. */
sealed interface StartupTaskResult {
    data object Success : StartupTaskResult

    data class Failure(
        val reason: StartupFailure,
    ) : StartupTaskResult
}

/** Stable runtime failure vocabulary safe to expose to bootstrap state. */
data class StartupFailure(
    val code: String,
    val recoverable: Boolean,
)
