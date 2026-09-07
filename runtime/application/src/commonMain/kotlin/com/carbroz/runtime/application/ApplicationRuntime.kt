package com.carbroz.runtime.application

import com.carbroz.runtime.application.startup.StartupBlocker
import com.carbroz.runtime.application.startup.StartupCoordinator
import com.carbroz.runtime.application.startup.StartupFailure
import com.carbroz.runtime.application.startup.StartupNotice
import com.carbroz.runtime.application.startup.StartupPayload
import com.carbroz.runtime.application.startup.StartupResolution
import com.carbroz.runtime.application.startup.StartupResult
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/** Single canonical owner of application startup state. */
interface ApplicationRuntime {
    val state: StateFlow<ApplicationRuntimeState>
    suspend fun start(): ApplicationRuntimeState
    suspend fun retry(): ApplicationRuntimeState
}

/** Product-neutral startup lifecycle exposed to presentation/composition. */
sealed interface ApplicationRuntimeState {
    data object Idle : ApplicationRuntimeState

    data class Starting(
        val attempt: UInt,
    ) : ApplicationRuntimeState

    data class Ready(
        val payload: StartupPayload,
        val notices: List<StartupNotice>,
        val attempt: UInt,
    ) : ApplicationRuntimeState

    data class Blocked(
        val blocker: StartupBlocker,
        val attempt: UInt,
    ) : ApplicationRuntimeState

    data class Failed(
        val taskId: String,
        val failure: StartupFailure,
        val attempt: UInt,
    ) : ApplicationRuntimeState
}

/** Serialized runtime implementation; no parallel bootstrap state is maintained elsewhere. */
class DefaultApplicationRuntime(
    private val startupCoordinator: StartupCoordinator,
) : ApplicationRuntime {
    private val operationMutex = Mutex()
    private val mutableState = MutableStateFlow<ApplicationRuntimeState>(ApplicationRuntimeState.Idle)

    override val state: StateFlow<ApplicationRuntimeState> = mutableState.asStateFlow()

    override suspend fun start(): ApplicationRuntimeState = operationMutex.withLock {
        when (val current = mutableState.value) {
            ApplicationRuntimeState.Idle -> executeAttempt(1u, current)
            else -> current
        }
    }

    override suspend fun retry(): ApplicationRuntimeState = operationMutex.withLock {
        when (val current = mutableState.value) {
            is ApplicationRuntimeState.Failed -> if (current.failure.recoverable) {
                executeAttempt(current.attempt + 1u, current)
            } else {
                current
            }

            is ApplicationRuntimeState.Blocked -> if (current.blocker.retryable) {
                executeAttempt(current.attempt + 1u, current)
            } else {
                current
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
                is StartupResult.Resolved -> result.resolution.toRuntimeState(attempt)
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

    private fun StartupResolution.toRuntimeState(attempt: UInt): ApplicationRuntimeState = when (this) {
        is StartupResolution.Ready -> ApplicationRuntimeState.Ready(
            payload = payload,
            notices = notices,
            attempt = attempt,
        )

        is StartupResolution.Blocked -> ApplicationRuntimeState.Blocked(
            blocker = blocker,
            attempt = attempt,
        )
    }
}
