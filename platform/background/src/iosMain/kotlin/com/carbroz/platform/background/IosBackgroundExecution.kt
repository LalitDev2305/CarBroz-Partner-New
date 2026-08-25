package com.carbroz.platform.background

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import platform.BackgroundTasks.BGAppRefreshTaskRequest
import platform.BackgroundTasks.BGProcessingTaskRequest
import platform.BackgroundTasks.BGTask
import platform.BackgroundTasks.BGTaskRequest
import platform.BackgroundTasks.BGTaskScheduler
import platform.Foundation.NSBundle
import platform.Foundation.NSDate
import platform.Foundation.NSLock
import kotlin.coroutines.resume

/**
 * Reads the host application's declared BGTaskScheduler identifiers without introducing a second
 * Kotlin configuration source. The plist remains the platform source of truth required by iOS.
 */
@OptIn(ExperimentalForeignApi::class)
fun iosPermittedBackgroundTaskIds(): Set<BackgroundTaskId> {
    val raw = NSBundle.mainBundle.objectForInfoDictionaryKey("BGTaskSchedulerPermittedIdentifiers") as? List<*>
        ?: return emptySet()
    return raw.mapNotNull { value ->
        (value as? String)?.let { runCatching { BackgroundTaskId(it) }.getOrNull() }
    }.toSet()
}

/**
 * iOS BGTaskScheduler adapter. Every schedulable identifier must be declared in
 * BGTaskSchedulerPermittedIdentifiers; failed native registrations are rejected explicitly.
 */
@OptIn(ExperimentalForeignApi::class)
class IosBackgroundScheduler(
    permittedTaskIds: Set<BackgroundTaskId>,
    private val runnerProvider: () -> BackgroundTaskRunner,
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default),
) : BackgroundScheduler {
    private val registeredIds = mutableSetOf<BackgroundTaskId>()
    private val runningJobs = mutableMapOf<BackgroundTaskId, Job>()
    private val lock = NSLock()

    init {
        permittedTaskIds.distinct().forEach { id ->
            val registered = BGTaskScheduler.sharedScheduler.registerForTaskWithIdentifier(
                identifier = id.value,
                usingQueue = null,
            ) { task -> task?.let { handle(id, it) } }
            if (registered) registeredIds += id
        }
    }

    override suspend fun schedule(request: BackgroundTaskRequest): BackgroundScheduleResult {
        if (request.id !in registeredIds) {
            return BackgroundScheduleResult.Rejected(
                "iOS background identifier ${request.id.value} is not declared or could not be registered",
            )
        }
        if (request.input.isNotEmpty()) {
            return BackgroundScheduleResult.Unsupported(
                "iOS BGTaskScheduler does not persist arbitrary task payloads; handlers must read durable application state",
            )
        }
        if (request.constraints.network == NetworkRequirement.UNMETERED) {
            return BackgroundScheduleResult.Unsupported("iOS BGTaskScheduler has no unmetered-network constraint")
        }
        if (request.constraints.requiresDeviceIdle) {
            return BackgroundScheduleResult.Unsupported("iOS BGTaskScheduler owns idle execution policy")
        }
        if (request.existingTaskPolicy == ExistingTaskPolicy.KEEP) {
            when (state(request.id)) {
                BackgroundTaskState.ENQUEUED,
                BackgroundTaskState.RUNNING,
                -> return BackgroundScheduleResult.AlreadyScheduled

                else -> Unit
            }
        }
        if (request.existingTaskPolicy == ExistingTaskPolicy.REPLACE) {
            BGTaskScheduler.sharedScheduler.cancelTaskRequestWithIdentifier(request.id.value)
        }

        val nativeRequest = when (request.kind) {
            BackgroundTaskKind.REFRESH -> BGAppRefreshTaskRequest(identifier = request.id.value)
            BackgroundTaskKind.PROCESSING -> BGProcessingTaskRequest(identifier = request.id.value).apply {
                requiresNetworkConnectivity = request.constraints.network == NetworkRequirement.CONNECTED
                requiresExternalPower = request.constraints.requiresCharging
            }
        }
        if (request.earliestStartDelayMillis > 0) {
            val delaySeconds = request.earliestStartDelayMillis.toDouble() / 1_000.0
            nativeRequest.earliestBeginDate = NSDate(
                timeIntervalSinceReferenceDate = NSDate().timeIntervalSinceReferenceDate + delaySeconds,
            )
        }
        return try {
            if (BGTaskScheduler.sharedScheduler.submitTaskRequest(nativeRequest, error = null)) {
                BackgroundScheduleResult.Scheduled
            } else {
                BackgroundScheduleResult.Rejected("iOS rejected background task ${request.id.value}")
            }
        } catch (failure: Throwable) {
            BackgroundScheduleResult.Rejected(failure.message ?: "iOS rejected background task")
        }
    }

    override suspend fun cancel(id: BackgroundTaskId) {
        locked { runningJobs.remove(id) }?.cancel()
        BGTaskScheduler.sharedScheduler.cancelTaskRequestWithIdentifier(id.value)
    }

    override suspend fun state(id: BackgroundTaskId): BackgroundTaskState = suspendCancellableCoroutine { continuation ->
        BGTaskScheduler.sharedScheduler.getPendingTaskRequestsWithCompletionHandler { requests ->
            val pending = requests
                ?.filterIsInstance<BGTaskRequest>()
                ?.any { request -> request.identifier == id.value } == true
            if (continuation.isActive) {
                continuation.resume(
                    when {
                        locked { runningJobs[id]?.isActive == true } -> BackgroundTaskState.RUNNING
                        pending -> BackgroundTaskState.ENQUEUED
                        else -> BackgroundTaskState.UNKNOWN
                    },
                )
            }
        }
    }

    private fun handle(id: BackgroundTaskId, task: BGTask) {
        val completionLock = NSLock()
        var completed = false
        fun complete(success: Boolean) {
            completionLock.lock()
            try {
                if (!completed) {
                    completed = true
                    task.setTaskCompletedWithSuccess(success)
                }
            } finally {
                completionLock.unlock()
            }
        }

        val job = scope.launch {
            try {
                val result = runnerProvider().run(id, emptyMap())
                complete(result == BackgroundExecutionResult.Success)
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (_: Throwable) {
                complete(false)
            }
        }
        locked { runningJobs[id] = job }
        task.expirationHandler = {
            job.cancel()
            complete(false)
        }
        job.invokeOnCompletion { locked { runningJobs.remove(id) } }
    }

    private fun <T> locked(block: () -> T): T {
        lock.lock()
        return try {
            block()
        } finally {
            lock.unlock()
        }
    }
}

/**
 * iOS does not expose a generic unlimited service equivalent. Operation-specific native modes or
 * continued-processing APIs must be selected only when their user-visible/product semantics apply.
 */
class IosContinuousExecutionController : ContinuousExecutionController {
    override suspend fun start(request: ContinuousExecutionRequest): ContinuousExecutionStartResult =
        ContinuousExecutionStartResult.Unsupported(
            "Generic continuous execution is not valid on iOS; use an operation-specific native background mode or continued-processing adapter",
        )

    override suspend fun stop(id: BackgroundTaskId) = Unit

    override suspend fun state(id: BackgroundTaskId): ContinuousExecutionState = ContinuousExecutionState.UNSUPPORTED
}
