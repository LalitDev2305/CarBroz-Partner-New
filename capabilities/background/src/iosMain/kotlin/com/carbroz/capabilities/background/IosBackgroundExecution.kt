package com.carbroz.capabilities.background

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import platform.BackgroundTasks.BGAppRefreshTaskRequest
import platform.BackgroundTasks.BGProcessingTask
import platform.BackgroundTasks.BGProcessingTaskRequest
import platform.BackgroundTasks.BGTask
import platform.BackgroundTasks.BGTaskScheduler
import platform.Foundation.NSDate
import kotlin.coroutines.resume

/**
 * iOS BGTaskScheduler adapter. Every schedulable identifier must be supplied by application
 * composition and declared in BGTaskSchedulerPermittedIdentifiers; unknown identifiers are rejected.
 */
@OptIn(ExperimentalForeignApi::class)
class IosBackgroundScheduler(
    permittedTaskIds: Set<BackgroundTaskId>,
    private val runnerProvider: () -> BackgroundTaskRunner,
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default),
) : BackgroundScheduler {
    private val permittedIds = permittedTaskIds.toSet()
    private val runningJobs = mutableMapOf<BackgroundTaskId, Job>()

    init {
        permittedIds.forEach { id ->
            BGTaskScheduler.sharedScheduler.registerForTaskWithIdentifier(
                identifier = id.value,
                usingQueue = null,
            ) { task -> handle(id, task) }
        }
    }

    override suspend fun schedule(request: BackgroundTaskRequest): BackgroundScheduleResult {
        if (request.id !in permittedIds) {
            return BackgroundScheduleResult.Rejected(
                "iOS background identifier ${request.id.value} is not registered/permitted",
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
        if (request.existingTaskPolicy == ExistingTaskPolicy.KEEP && state(request.id) == BackgroundTaskState.ENQUEUED) {
            return BackgroundScheduleResult.AlreadyScheduled
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
            nativeRequest.earliestBeginDate = NSDate.dateWithTimeIntervalSinceNow(
                request.earliestStartDelayMillis.toDouble() / 1_000.0,
            )
        }
        return try {
            BGTaskScheduler.sharedScheduler.submitTaskRequest(nativeRequest, error = null)
            BackgroundScheduleResult.Scheduled
        } catch (failure: Throwable) {
            BackgroundScheduleResult.Rejected(failure.message ?: "iOS rejected background task")
        }
    }

    override suspend fun cancel(id: BackgroundTaskId) {
        runningJobs.remove(id)?.cancel()
        BGTaskScheduler.sharedScheduler.cancelTaskRequestWithIdentifier(id.value)
    }

    override suspend fun state(id: BackgroundTaskId): BackgroundTaskState = suspendCancellableCoroutine { continuation ->
        BGTaskScheduler.sharedScheduler.getPendingTaskRequestsWithCompletionHandler { requests ->
            val pending = requests.any { request -> request.identifier == id.value }
            if (continuation.isActive) {
                continuation.resume(
                    when {
                        runningJobs[id]?.isActive == true -> BackgroundTaskState.RUNNING
                        pending -> BackgroundTaskState.ENQUEUED
                        else -> BackgroundTaskState.UNKNOWN
                    },
                )
            }
        }
    }

    private fun handle(id: BackgroundTaskId, task: BGTask) {
        val job = scope.launch {
            val result = runnerProvider().run(id, emptyMap())
            task.setTaskCompletedWithSuccess(result == BackgroundExecutionResult.Success)
        }
        runningJobs[id] = job
        task.expirationHandler = { job.cancel() }
        job.invokeOnCompletion { runningJobs.remove(id) }
    }
}

/**
 * iOS does not expose a generic unlimited service equivalent. Operation-specific native modes or
 * BGContinuedProcessing must be selected only when their user-visible/product semantics apply.
 */
class IosContinuousExecutionController : ContinuousExecutionController {
    override suspend fun start(request: ContinuousExecutionRequest): ContinuousExecutionStartResult =
        ContinuousExecutionStartResult.Unsupported(
            "Generic continuous execution is not valid on iOS; use an operation-specific native background mode or continued-processing adapter",
        )

    override suspend fun stop(id: BackgroundTaskId) = Unit

    override suspend fun state(id: BackgroundTaskId): ContinuousExecutionState = ContinuousExecutionState.UNSUPPORTED
}
