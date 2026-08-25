package com.carbroz.platform.background

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.concurrent.ConcurrentHashMap

/**
 * Android foreground-service controller for genuinely user-visible continuous work.
 *
 * The generic adapter intentionally owns a single lease. Operations needing simultaneous or
 * operation-specific foreground-service types must supply a dedicated native adapter rather than
 * multiplexing unrelated semantics through this generic special-use service.
 */
class AndroidContinuousExecutionController(private val context: Context) : ContinuousExecutionController {
    private val appContext = context.applicationContext
    private val mutex = Mutex()

    override suspend fun start(request: ContinuousExecutionRequest): ContinuousExecutionStartResult = mutex.withLock {
        if (CarBrozContinuousExecutionService.isRunning(request.id)) {
            return ContinuousExecutionStartResult.AlreadyRunning
        }
        if (CarBrozContinuousExecutionService.hasRunningLease()) {
            return ContinuousExecutionStartResult.Rejected(
                "Generic Android continuous execution already owns another foreground lease",
            )
        }
        val intent = Intent(appContext, CarBrozContinuousExecutionService::class.java)
            .setAction(CarBrozContinuousExecutionService.ACTION_START)
            .putExtra(CarBrozContinuousExecutionService.EXTRA_ID, request.id.value)
            .putExtra(CarBrozContinuousExecutionService.EXTRA_TITLE, request.title)
            .putExtra(CarBrozContinuousExecutionService.EXTRA_DESCRIPTION, request.description)
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                appContext.startForegroundService(intent)
            } else {
                appContext.startService(intent)
            }
            ContinuousExecutionStartResult.Started
        } catch (failure: RuntimeException) {
            ContinuousExecutionStartResult.Rejected(failure.message ?: "Android rejected foreground execution")
        }
    }

    override suspend fun stop(id: BackgroundTaskId) = mutex.withLock {
        if (!CarBrozContinuousExecutionService.isRunning(id)) return@withLock
        CarBrozContinuousExecutionService.markStopped(id)
        appContext.stopService(Intent(appContext, CarBrozContinuousExecutionService::class.java))
        Unit
    }

    override suspend fun state(id: BackgroundTaskId): ContinuousExecutionState =
        if (CarBrozContinuousExecutionService.isRunning(id)) ContinuousExecutionState.RUNNING else ContinuousExecutionState.STOPPED
}

/** Host service only maintains the foreground execution lease; operation logic remains outside the service. */
class CarBrozContinuousExecutionService : Service() {
    override fun onCreate() {
        super.onCreate()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(
                NotificationChannel(CHANNEL_ID, "Ongoing operations", NotificationManager.IMPORTANCE_LOW),
            )
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> start(intent)
            else -> stopSelf(startId)
        }
        return START_NOT_STICKY
    }

    private fun start(intent: Intent) {
        val rawId = intent.getStringExtra(EXTRA_ID) ?: return stopSelf()
        val id = runCatching { BackgroundTaskId(rawId) }.getOrNull() ?: return stopSelf()
        val title = intent.getStringExtra(EXTRA_TITLE).orEmpty()
        val description = intent.getStringExtra(EXTRA_DESCRIPTION).orEmpty()
        if (title.isBlank() || description.isBlank()) return stopSelf()
        if (running.isNotEmpty() && id !in running) return stopSelf()
        running += id
        val notification = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Notification.Builder(this, CHANNEL_ID)
        } else {
            @Suppress("DEPRECATION")
            Notification.Builder(this)
        }
            .setSmallIcon(android.R.drawable.stat_notify_sync)
            .setContentTitle(title)
            .setContentText(description)
            .setOngoing(true)
            .build()
        if (Build.VERSION.SDK_INT >= 34) {
            startForeground(notificationId(id), notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        } else {
            startForeground(notificationId(id), notification)
        }
    }

    override fun onDestroy() {
        running.clear()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        internal const val ACTION_START = "com.carbroz.platform.background.START"
        internal const val EXTRA_ID = "id"
        internal const val EXTRA_TITLE = "title"
        internal const val EXTRA_DESCRIPTION = "description"
        private const val CHANNEL_ID = "carbroz_continuous_execution"
        private val running = ConcurrentHashMap.newKeySet<BackgroundTaskId>()

        internal fun isRunning(id: BackgroundTaskId): Boolean = id in running
        internal fun hasRunningLease(): Boolean = running.isNotEmpty()
        internal fun markStopped(id: BackgroundTaskId) { running -= id }
        private fun notificationId(id: BackgroundTaskId): Int = id.value.hashCode().and(0x7fffffff).coerceAtLeast(1)
    }
}
