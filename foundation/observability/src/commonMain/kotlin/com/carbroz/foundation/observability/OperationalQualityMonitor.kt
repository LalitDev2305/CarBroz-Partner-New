package com.carbroz.foundation.observability

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.time.TimeSource

/** Posts a heartbeat to the platform UI/main thread without exposing native dispatcher APIs to common code. */
fun interface MainThreadDispatcher {
    fun dispatch(block: () -> Unit)
}

sealed interface ResponsivenessMonitorStartResult {
    data object Started : ResponsivenessMonitorStartResult
    data object AlreadyStarted : ResponsivenessMonitorStartResult
}

/** Process-lifetime watchdog for main/UI thread stalls. */
class MainThreadResponsivenessMonitor(
    private val dispatcher: MainThreadDispatcher,
    private val observability: Observability,
    private val thresholdMillis: Long = 5_000,
    private val intervalMillis: Long = 1_000,
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default),
) {
    private var monitoringJob: Job? = null

    init {
        require(thresholdMillis > 0) { "Responsiveness threshold must be positive." }
        require(intervalMillis > 0) { "Responsiveness interval must be positive." }
    }

    fun start(): ResponsivenessMonitorStartResult {
        if (monitoringJob?.isActive == true) return ResponsivenessMonitorStartResult.AlreadyStarted
        monitoringJob = scope.launch {
            while (isActive) {
                val heartbeat = CompletableDeferred<Unit>()
                val started = TimeSource.Monotonic.markNow()
                dispatcher.dispatch { heartbeat.complete(Unit) }
                val responsive = withTimeoutOrNull(thresholdMillis) {
                    heartbeat.await()
                    true
                } ?: false
                if (!responsive) {
                    observability.responsiveness(
                        ResponsivenessIncident(
                            scope = "main_thread",
                            blockedMillis = started.elapsedNow().inWholeMilliseconds.coerceAtLeast(thresholdMillis),
                            thresholdMillis = thresholdMillis,
                        ),
                    )
                }
                delay(intervalMillis)
            }
        }
        return ResponsivenessMonitorStartResult.Started
    }

    fun stop() {
        monitoringJob?.cancel()
        monitoringJob = null
    }

    /** Cancels the owned process scope; the monitor cannot be restarted after this call. */
    fun close() {
        monitoringJob = null
        scope.cancel()
    }
}

/** Samples platform resource state and emits only typed, bounded diagnostics. */
class ResourceDiagnosticsReporter(
    private val diagnostics: ResourceDiagnostics,
    private val observability: Observability,
) {
    fun sample(): ResourceDiagnosticResult {
        val result = runCatching { diagnostics.sample() }
            .getOrElse { ResourceDiagnosticResult.Unavailable("resource sampling failed") }
        when (result) {
            is ResourceDiagnosticResult.Available -> observability.resource(result.snapshot)
            is ResourceDiagnosticResult.Unsupported -> observability.log(
                LogEvent(
                    level = LogLevel.DEBUG,
                    category = "resources",
                    message = "resource_sampling_unsupported",
                ),
            )
            is ResourceDiagnosticResult.Unavailable -> observability.log(
                LogEvent(
                    level = LogLevel.WARN,
                    category = "resources",
                    message = "resource_sampling_unavailable",
                ),
            )
        }
        return result
    }
}
