package com.carbroz.platform.background

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.koin.mp.KoinPlatform
import java.util.concurrent.TimeUnit

/** Android reliable deferred-work adapter backed by WorkManager. */
class AndroidBackgroundScheduler(context: Context) : BackgroundScheduler {
    private val workManager = WorkManager.getInstance(context.applicationContext)
    private val mutex = Mutex()

    override suspend fun schedule(request: BackgroundTaskRequest): BackgroundScheduleResult = withContext(Dispatchers.IO) {
        mutex.withLock {
            if (request.existingTaskPolicy == ExistingTaskPolicy.KEEP && activeState(request.id) != null) {
                return@withLock BackgroundScheduleResult.AlreadyScheduled
            }

            val data = Data.Builder().putString(KEY_TASK_ID, request.id.value).apply {
                request.input.forEach { (key, value) -> putString(INPUT_PREFIX + key, value) }
            }.build()
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(
                    when (request.constraints.network) {
                        NetworkRequirement.NOT_REQUIRED -> NetworkType.NOT_REQUIRED
                        NetworkRequirement.CONNECTED -> NetworkType.CONNECTED
                        NetworkRequirement.UNMETERED -> NetworkType.UNMETERED
                    },
                )
                .setRequiresCharging(request.constraints.requiresCharging)
                .setRequiresDeviceIdle(request.constraints.requiresDeviceIdle)
                .build()
            val work = OneTimeWorkRequestBuilder<CarBrozBackgroundWorker>()
                .setInputData(data)
                .setConstraints(constraints)
                .setInitialDelay(request.earliestStartDelayMillis, TimeUnit.MILLISECONDS)
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
                .addTag(tag(request.id))
                .build()
            workManager.enqueueUniqueWork(
                uniqueName(request.id),
                when (request.existingTaskPolicy) {
                    ExistingTaskPolicy.KEEP -> ExistingWorkPolicy.KEEP
                    ExistingTaskPolicy.REPLACE -> ExistingWorkPolicy.REPLACE
                },
                work,
            )
            BackgroundScheduleResult.Scheduled
        }
    }

    override suspend fun cancel(id: BackgroundTaskId) = withContext(Dispatchers.IO) {
        mutex.withLock {
            workManager.cancelUniqueWork(uniqueName(id))
            Unit
        }
    }

    override suspend fun state(id: BackgroundTaskId): BackgroundTaskState = withContext(Dispatchers.IO) {
        mutex.withLock { currentState(id) }
    }

    private fun activeState(id: BackgroundTaskId): WorkInfo.State? =
        workManager.getWorkInfosForUniqueWork(uniqueName(id)).get().lastOrNull()?.state
            ?.takeIf { it == WorkInfo.State.ENQUEUED || it == WorkInfo.State.BLOCKED || it == WorkInfo.State.RUNNING }

    private fun currentState(id: BackgroundTaskId): BackgroundTaskState =
        workManager.getWorkInfosForUniqueWork(uniqueName(id)).get().lastOrNull()?.state.toSemanticState()

    private fun WorkInfo.State?.toSemanticState(): BackgroundTaskState = when (this) {
        WorkInfo.State.ENQUEUED, WorkInfo.State.BLOCKED -> BackgroundTaskState.ENQUEUED
        WorkInfo.State.RUNNING -> BackgroundTaskState.RUNNING
        WorkInfo.State.SUCCEEDED -> BackgroundTaskState.SUCCEEDED
        WorkInfo.State.FAILED -> BackgroundTaskState.FAILED
        WorkInfo.State.CANCELLED -> BackgroundTaskState.CANCELLED
        null -> BackgroundTaskState.UNKNOWN
    }

    companion object {
        internal const val KEY_TASK_ID = "carbroz.background.task_id"
        internal const val INPUT_PREFIX = "carbroz.background.input."
        private fun uniqueName(id: BackgroundTaskId) = "carbroz.background.${id.value}"
        private fun tag(id: BackgroundTaskId) = "carbroz.background.tag.${id.value}"
    }
}

/** Generic WorkManager entry point; business operations are resolved through the canonical Koin graph. */
class CarBrozBackgroundWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params) {
    override suspend fun doWork(): Result {
        val rawId = inputData.getString(AndroidBackgroundScheduler.KEY_TASK_ID) ?: return Result.failure()
        val id = runCatching { BackgroundTaskId(rawId) }.getOrNull() ?: return Result.failure()
        val input = inputData.keyValueMap.entries
            .asSequence()
            .filter { it.key.startsWith(AndroidBackgroundScheduler.INPUT_PREFIX) }
            .mapNotNull { (key, value) ->
                (value as? String)?.let { key.removePrefix(AndroidBackgroundScheduler.INPUT_PREFIX) to it }
            }
            .toMap()
        val runner = KoinPlatform.getKoin().get<BackgroundTaskRunner>()
        return when (runner.run(id, input)) {
            BackgroundExecutionResult.Success -> Result.success()
            BackgroundExecutionResult.Retry -> Result.retry()
            is BackgroundExecutionResult.Failure -> Result.failure()
        }
    }
}
