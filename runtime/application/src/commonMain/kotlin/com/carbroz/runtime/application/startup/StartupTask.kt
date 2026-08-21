package com.carbroz.runtime.application.startup

/**
 * One ordered unit of application bootstrap work.
 *
 * Tasks must be product-neutral at the runtime layer and return a typed result
 * for expected bootstrap failures. Unexpected exceptions are contained by the
 * coordinator so platform or library failures cannot escape into splash UI.
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

/**
 * Stable runtime failure vocabulary exposed by bootstrap.
 *
 * [Expected] carries a machine-readable code supplied by the task. [Unexpected]
 * intentionally carries no Throwable: raw exception details belong in the
 * observability layer, not application state or UI.
 */
sealed interface StartupFailure {
    val recoverable: Boolean

    data class Expected(
        val code: String,
        override val recoverable: Boolean,
    ) : StartupFailure

    data object Unexpected : StartupFailure {
        override val recoverable: Boolean = false
    }
}
