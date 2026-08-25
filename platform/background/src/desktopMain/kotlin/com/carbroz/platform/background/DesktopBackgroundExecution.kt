package com.carbroz.platform.background

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.concurrent.ConcurrentHashMap

/**
 * Desktop policy: scheduled work is reliable only while this process is alive. OS-level network,
 * charging and idle constraints are rejected rather than silently ignored.
 */
class DesktopBackgroundScheduler(
    private val runnerProvider: () -> BackgroundTaskRunner,
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default),
) : BackgroundScheduler {
    private val mutex = Mutex()
    private val jobs = ConcurrentHashMap<BackgroundTaskId, Job>()
    private val states = ConcurrentHashMap<BackgroundTaskId, BackgroundTaskState>()

    override suspend fun schedule(request: BackgroundTaskRequest): BackgroundScheduleResult = mutex.withLock {
        if (request.constraints != BackgroundConstraints()) {
            return BackgroundScheduleResult.Unsupported(
                "Desktop process scheduler cannot guarantee network, charging or device-idle constraints",
            )
        }
        val existing = jobs[request.id]
        if (existing?.isActive == true && request.existingTaskPolicy == ExistingTaskPolicy.KEEP) {
            return BackgroundScheduleResult.AlreadyScheduled
        }
        existing?.cancel()
        states[request.id] = BackgroundTaskState.ENQUEUED
        val job = scope.launch {
            delay(request.earliestStartDelayMillis)
            states[request.id] = BackgroundTaskState.RUNNING
            states[request.id] = when (runnerProvider().run(request.id, request.input)) {
                BackgroundExecutionResult.Success -> BackgroundTaskState.SUCCEEDED
                BackgroundExecutionResult.Retry -> BackgroundTaskState.FAILED
                is BackgroundExecutionResult.Failure -> BackgroundTaskState.FAILED
            }
        }
        jobs[request.id] = job
        job.invokeOnCompletion { cause ->
            if (cause is CancellationException) states[request.id] = BackgroundTaskState.CANCELLED
            jobs.remove(request.id, job)
        }
        BackgroundScheduleResult.Scheduled
    }

    override suspend fun cancel(id: BackgroundTaskId) = mutex.withLock {
        jobs.remove(id)?.cancel()
        states[id] = BackgroundTaskState.CANCELLED
    }

    override suspend fun state(id: BackgroundTaskId): BackgroundTaskState =
        states[id] ?: BackgroundTaskState.UNKNOWN
}

/** Desktop continuous execution is process-scoped and has no OS persistent-service guarantee. */
class DesktopContinuousExecutionController : ContinuousExecutionController {
    private val mutex = Mutex()
    private val running = mutableSetOf<BackgroundTaskId>()

    override suspend fun start(request: ContinuousExecutionRequest): ContinuousExecutionStartResult = mutex.withLock {
        if (!running.add(request.id)) ContinuousExecutionStartResult.AlreadyRunning else ContinuousExecutionStartResult.Started
    }

    override suspend fun stop(id: BackgroundTaskId) = mutex.withLock { running.remove(id); Unit }

    override suspend fun state(id: BackgroundTaskId): ContinuousExecutionState = mutex.withLock {
        if (id in running) ContinuousExecutionState.RUNNING else ContinuousExecutionState.STOPPED
    }
}
