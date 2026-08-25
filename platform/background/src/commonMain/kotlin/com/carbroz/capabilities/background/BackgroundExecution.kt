package com.carbroz.platform.background

import kotlinx.coroutines.CancellationException

/** Stable semantic identifier for a registered background operation. */
data class BackgroundTaskId(val value: String) {
    init {
        require(value.isNotBlank()) { "BackgroundTaskId must not be blank" }
        require(value.length <= MAX_LENGTH) { "BackgroundTaskId must be at most $MAX_LENGTH characters" }
    }

    companion object {
        const val MAX_LENGTH: Int = 120
    }
}

/** Scheduling class; platform adapters translate this without leaking native scheduler types. */
enum class BackgroundTaskKind { REFRESH, PROCESSING }

enum class NetworkRequirement { NOT_REQUIRED, CONNECTED, UNMETERED }

data class BackgroundConstraints(
    val network: NetworkRequirement = NetworkRequirement.NOT_REQUIRED,
    val requiresCharging: Boolean = false,
    val requiresDeviceIdle: Boolean = false,
)

enum class ExistingTaskPolicy { KEEP, REPLACE }

data class BackgroundTaskRequest(
    val id: BackgroundTaskId,
    val kind: BackgroundTaskKind,
    val earliestStartDelayMillis: Long = 0,
    val constraints: BackgroundConstraints = BackgroundConstraints(),
    val existingTaskPolicy: ExistingTaskPolicy = ExistingTaskPolicy.KEEP,
    val input: Map<String, String> = emptyMap(),
) {
    init {
        require(earliestStartDelayMillis >= 0) { "earliestStartDelayMillis must be non-negative" }
        require(input.size <= MAX_INPUT_ENTRIES) { "Too many background task input entries" }
        require(input.keys.all { it.isNotBlank() && it.length <= MAX_INPUT_KEY_LENGTH }) {
            "Background task input keys must be non-blank and bounded"
        }
        require(input.values.all { it.length <= MAX_INPUT_VALUE_LENGTH }) {
            "Background task input values must be bounded"
        }
    }

    companion object {
        const val MAX_INPUT_ENTRIES: Int = 32
        const val MAX_INPUT_KEY_LENGTH: Int = 80
        const val MAX_INPUT_VALUE_LENGTH: Int = 2_048
    }
}

sealed interface BackgroundScheduleResult {
    data object Scheduled : BackgroundScheduleResult
    data object AlreadyScheduled : BackgroundScheduleResult
    data class Unsupported(val reason: String) : BackgroundScheduleResult
    data class Rejected(val reason: String) : BackgroundScheduleResult
}

enum class BackgroundTaskState { ENQUEUED, RUNNING, SUCCEEDED, FAILED, CANCELLED, UNKNOWN }

/** Canonical scheduler boundary for reliable, deferred work. */
interface BackgroundScheduler {
    suspend fun schedule(request: BackgroundTaskRequest): BackgroundScheduleResult
    suspend fun cancel(id: BackgroundTaskId)
    suspend fun state(id: BackgroundTaskId): BackgroundTaskState
}

sealed interface BackgroundExecutionResult {
    data object Success : BackgroundExecutionResult
    data object Retry : BackgroundExecutionResult
    data class Failure(val reason: String) : BackgroundExecutionResult
}

/** Application/domain-owned operation executed by a platform scheduler adapter. */
interface BackgroundTaskHandler {
    val id: BackgroundTaskId
    suspend fun execute(input: Map<String, String>): BackgroundExecutionResult
}

/** Immutable handler registry; duplicate identifiers fail at composition time. */
class BackgroundTaskHandlerRegistry(handlers: List<BackgroundTaskHandler>) {
    private val handlersById = handlers.associateBy { it.id }.also { indexed ->
        require(indexed.size == handlers.size) { "Duplicate background task handler id" }
    }

    fun handler(id: BackgroundTaskId): BackgroundTaskHandler? = handlersById[id]
}

/** Shared execution policy used by native worker callbacks. */
class BackgroundTaskRunner(private val registry: BackgroundTaskHandlerRegistry) {
    suspend fun run(id: BackgroundTaskId, input: Map<String, String>): BackgroundExecutionResult =
        try {
            registry.handler(id)?.execute(input)
                ?: BackgroundExecutionResult.Failure("No handler registered for ${id.value}")
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (failure: Throwable) {
            BackgroundExecutionResult.Failure(failure.message ?: failure::class.simpleName ?: "background task failed")
        }
}

/** Semantic request for genuine user-visible continuous execution, separate from deferred work. */
data class ContinuousExecutionRequest(
    val id: BackgroundTaskId,
    val title: String,
    val description: String,
) {
    init {
        require(title.isNotBlank() && title.length <= MAX_TITLE_LENGTH) {
            "Continuous execution title must be non-blank and bounded"
        }
        require(description.isNotBlank() && description.length <= MAX_DESCRIPTION_LENGTH) {
            "Continuous execution description must be non-blank and bounded"
        }
    }

    companion object {
        const val MAX_TITLE_LENGTH: Int = 120
        const val MAX_DESCRIPTION_LENGTH: Int = 512
    }
}

enum class ContinuousExecutionState { STOPPED, STARTING, RUNNING, STOPPING, UNSUPPORTED }

sealed interface ContinuousExecutionStartResult {
    data object Started : ContinuousExecutionStartResult
    data object AlreadyRunning : ContinuousExecutionStartResult
    data class Unsupported(val reason: String) : ContinuousExecutionStartResult
    data class Rejected(val reason: String) : ContinuousExecutionStartResult
}

/** Boundary for genuinely continuous execution. It must not be implemented by ordinary deferred scheduling. */
interface ContinuousExecutionController {
    suspend fun start(request: ContinuousExecutionRequest): ContinuousExecutionStartResult
    suspend fun stop(id: BackgroundTaskId)
    suspend fun state(id: BackgroundTaskId): ContinuousExecutionState
}
