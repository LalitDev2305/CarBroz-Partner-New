package com.carbroz.runtime.application

import com.carbroz.runtime.application.startup.StartupCoordinator
import com.carbroz.runtime.application.startup.StartupFailure
import com.carbroz.runtime.application.startup.StartupResult
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Read-only application bootstrap state exposed to UI and navigation layers.
 *
 * The runtime is the single canonical owner of application startup state. UI
 * consumers observe [state] and request [start] or [retry]; they never mutate
 * bootstrap state directly.
 */
interface ApplicationRuntime {
    val state: StateFlow<ApplicationRuntimeState>

    /** Performs the initial bootstrap if the runtime has not already started. */
    suspend fun start(): ApplicationRuntimeState

    /** Retries a previously failed bootstrap only when the failure is recoverable. */
    suspend fun retry(): ApplicationRuntimeState
}

/** Stable product-neutral states for application bootstrap. */
sealed interface ApplicationRuntimeState {
    data object Idle : ApplicationRuntimeState

    data class Starting(
        val attempt: UInt,
    ) : ApplicationRuntimeState

    data class Ready(
        val attempt: UInt,
    ) : ApplicationRuntimeState

    data class Failed(
        val taskId: String,
        val failure: StartupFailure,
        val attempt: UInt,
    ) : ApplicationRuntimeState
}

/**
 * Default serialized [ApplicationRuntime] implementation.
 *
 * A mutex guarantees that concurrent startup/retry callers cannot execute the
 * bootstrap pipeline more than once at the same time. The coordinator remains
 * stateless; this class alone owns observable startup lifecycle state.
 *
 * Cancellation never leaves the runtime stranded in [ApplicationRuntimeState.Starting].
 * The previous stable state is restored before cancellation is propagated.
 */
class DefaultApplicationRuntime(
    private val startupCoordinator: StartupCoordinator,
) : ApplicationRuntime {
    private val operationMutex = Mutex()
    private val mutableState = MutableStateFlow<ApplicationRuntimeState>(ApplicationRuntimeState.Idle)

    override val state: StateFlow<ApplicationRuntimeState> = mutableState.asStateFlow()

    override suspend fun start(): ApplicationRuntimeState = operationMutex.withLock {
        when (val current = mutableState.value) {
            ApplicationRuntimeState.Idle -> executeAttempt(
                attempt = 1u,
                fallbackState = current,
            )

            else -> current
        }
    }

    override suspend fun retry(): ApplicationRuntimeState = operationMutex.withLock {
        when (val current = mutableState.value) {
            is ApplicationRuntimeState.Failed -> {
                if (current.failure.recoverable) {
                    executeAttempt(
                        attempt = current.attempt + 1u,
                        fallbackState = current,
                    )
                } else {
                    current
                }
            }

            else -> current
        }
    }

    private suspend fun executeAttempt(
        attempt: UInt,
        fallbackState: ApplicationRuntimeState,
    ): ApplicationRuntimeState {
        mutableState.value = ApplicationRuntimeState.Starting(attempt)

        val next = try {
            when (val result = startupCoordinator.run()) {
                StartupResult.Ready -> ApplicationRuntimeState.Ready(attempt)
                is StartupResult.Failed -> ApplicationRuntimeState.Failed(
                    taskId = result.taskId,
                    failure = result.failure,
                    attempt = attempt,
                )
            }
        } catch (cancellation: CancellationException) {
            mutableState.value = fallbackState
            throw cancellation
        }

        mutableState.value = next
        return next
    }
}
