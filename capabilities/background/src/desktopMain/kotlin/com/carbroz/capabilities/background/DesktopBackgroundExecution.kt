package com.carbroz.capabilities.background

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/** Desktop policy: reliable while this process is alive; explicitly not persistent across process exit. */
class DesktopBackgroundScheduler(
    private val runner: BackgroundTaskRunner,
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default),
) : BackgroundScheduler {
    private val mutex = Mutex()
    private val jobs = mutableMapOf<BackgroundTaskId, Job>()
    private val states = mutableMapOf<BackgroundTaskId, BackgroundTaskState>()

    override suspend fun schedule(request: BackgroundTaskRequest): BackgroundScheduleResult = mutex.withLock {
        val existing = jobs[request.id]
        if (existing?.isActive == true && request.existingTaskPolicy == ExistingTaskPolicy.KEEP) {
            return BackgroundScheduleResult.AlreadyScheduled
        }
        existing?.cancel()
        states[request.id] = BackgroundTaskState.ENQUEUED
        jobs[request.id] = scope.launch {
            delay(request.earliestStartDelayMillis)
            states[request.id] = BackgroundTaskState.RUNNING
            states[request.id] = when (runner.run(request.id, request.input)) {
                BackgroundExecutionResult.Success -> BackgroundTaskState.SUCCEEDED
                BackgroundExecutionResult.Retry -> BackgroundTaskState.FAILED
                is BackgroundExecutionResult.Failure -> BackgroundTaskState.FAILED
            }
        }.also { job ->
            job.invokeOnCompletion { cause ->
                if (cause is kotlinx.coroutines.CancellationException) states[request.id] = BackgroundTaskState.CANCELLED
            }
        }
        BackgroundScheduleResult.Scheduled
    }

    override suspend fun cancel(id: BackgroundTaskId) = mutex.withLock {
        jobs.remove(id)?.cancel()
        states[id] = BackgroundTaskState.CANCELLED
    }

    override suspend fun state(id: BackgroundTaskId): BackgroundTaskState = mutex.withLock {
        states[id] ?: BackgroundTaskState.UNKNOWN
    }
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
