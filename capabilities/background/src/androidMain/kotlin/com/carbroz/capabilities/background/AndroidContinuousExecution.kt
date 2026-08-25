package com.carbroz.capabilities.background

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

/** Android foreground-service controller for genuinely user-visible continuous work. */
class AndroidContinuousExecutionController(private val context: Context) : ContinuousExecutionController {
    private val appContext = context.applicationContext
    private val mutex = Mutex()

    override suspend fun start(request: ContinuousExecutionRequest): ContinuousExecutionStartResult = mutex.withLock {
        if (CarBrozContinuousExecutionService.isRunning(request.id)) {
            return ContinuousExecutionStartResult.AlreadyRunning
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
        appContext.startService(
            Intent(appContext, CarBrozContinuousExecutionService::class.java)
                .setAction(CarBrozContinuousExecutionService.ACTION_STOP)
                .putExtra(CarBrozContinuousExecutionService.EXTRA_ID, id.value),
        )
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
            ACTION_STOP -> stop(intent.getStringExtra(EXTRA_ID))
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

    private fun stop(rawId: String?) {
        val id = rawId?.let { runCatching { BackgroundTaskId(it) }.getOrNull() }
        if (id != null) running -= id
        if (running.isEmpty()) {
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
        }
    }

    override fun onDestroy() {
        running.clear()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        internal const val ACTION_START = "com.carbroz.background.START"
        internal const val ACTION_STOP = "com.carbroz.background.STOP"
        internal const val EXTRA_ID = "id"
        internal const val EXTRA_TITLE = "title"
        internal const val EXTRA_DESCRIPTION = "description"
        private const val CHANNEL_ID = "carbroz_continuous_execution"
        private val running = ConcurrentHashMap.newKeySet<BackgroundTaskId>()

        internal fun isRunning(id: BackgroundTaskId): Boolean = id in running
        private fun notificationId(id: BackgroundTaskId): Int = id.value.hashCode().and(0x7fffffff).coerceAtLeast(1)
    }
}
