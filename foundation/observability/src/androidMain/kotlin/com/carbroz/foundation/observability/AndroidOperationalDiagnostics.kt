package com.carbroz.foundation.observability

import android.os.Handler
import android.os.Looper
import android.util.Log

/** Android main-loop heartbeat adapter used by the common responsiveness watchdog. */
class AndroidMainThreadDispatcher : MainThreadDispatcher {
    private val handler = Handler(Looper.getMainLooper())

    override fun dispatch(block: () -> Unit) {
        handler.post(block)
    }
}

/** Lightweight process resource sampling backed by the Android/JVM runtime. */
class AndroidResourceDiagnostics : ResourceDiagnostics {
    override fun sample(): ResourceDiagnosticResult = runCatching {
        val runtime = Runtime.getRuntime()
        ResourceDiagnosticResult.Available(
            ResourceSnapshot(
                heapUsedBytes = (runtime.totalMemory() - runtime.freeMemory()).coerceAtLeast(0L),
                heapLimitBytes = runtime.maxMemory().coerceAtLeast(0L),
                processorCount = runtime.availableProcessors().coerceAtLeast(1),
            ),
        )
    }.getOrElse {
        ResourceDiagnosticResult.Unavailable("android runtime resource sampling failed")
    }
}

/**
 * Curated Android Logcat diagnostics for Development/Staging only.
 *
 * Logcat severity supplies native coloring. The local stream is intentionally limited to startup FLOW and
 * pretty API request/response/error blocks; metrics, traces, responsiveness and resource samples stay silent.
 */
class AndroidPlatformDiagnosticSink(
    private val tag: String = "CarBroz",
) : LogSink, CrashSink, PerformanceSink, TraceSink, ResponsivenessSink, ResourceSink, DiagnosticBlockSink {

    override fun emit(event: LogEvent) {
        val message = renderFlow(event) ?: return
        when (event.level) {
            LogLevel.DEBUG -> Log.d(tag, message)
            LogLevel.INFO -> Log.i(tag, message)
            LogLevel.WARN -> Log.w(tag, message)
            LogLevel.ERROR -> Log.e(tag, message)
        }
    }

    override fun emit(block: DiagnosticBlock) {
        val label = when (block.kind) {
            DiagnosticBlockKind.API_REQUEST -> "🌐 API REQUEST"
            DiagnosticBlockKind.API_RESPONSE -> "✅ API RESPONSE"
            DiagnosticBlockKind.API_ERROR -> "❌ API ERROR"
        }
        val rendered = buildString {
            appendLine("╭─ $label  ${block.title}")
            block.content.lineSequence().forEach { line -> appendLine("│ $line") }
            append("╰────────────────────────────────────────────────────────")
        }
        when (block.kind) {
            DiagnosticBlockKind.API_REQUEST -> Log.d(tag, rendered)
            DiagnosticBlockKind.API_RESPONSE -> Log.i(tag, rendered)
            DiagnosticBlockKind.API_ERROR -> Log.e(tag, rendered)
        }
    }

    override fun record(event: CrashEvent, throwable: Throwable?) {
        if (event.category != STARTUP_CATEGORY) return
        Log.e(tag, "❌ FLOW ERROR  ${event.category}.${event.message}${correlationSuffix(event.correlationId)}")
    }

    override fun record(metric: PerformanceMetric) = Unit
    override fun record(span: TraceSpan) = Unit
    override fun record(incident: ResponsivenessIncident) = Unit
    override fun record(snapshot: ResourceSnapshot) = Unit

    private fun renderFlow(event: LogEvent): String? {
        if (event.category != STARTUP_CATEGORY) return null
        val taskId = event.attributes["task_id"]?.value
        val message = when (event.message) {
            "startup_started" ->
                "AppLifecycle.Foreground → SplashStore → ApplicationRuntime.start() → StartupCoordinator.run()"
            "startup_task_started" -> when (taskId) {
                "session.restore" -> "StartupCoordinator.run() → SessionRestoreStartupTask.execute()"
                "bootstrap" -> "StartupCoordinator.run() → BootstrapStartupTask.execute() → ResolveBootstrapUseCase.invoke() → RemoteBootstrapRepository.load()"
                else -> "StartupCoordinator.run() → StartupTask.execute(task=$taskId)"
            }
            "startup_ready" -> "Bootstrap resolved → ApplicationRuntime.Ready → SplashEffect.Navigate"
            "startup_blocked" -> "Bootstrap resolved → ApplicationRuntime.Blocked"
            "startup_task_failed" -> "Startup task failed(task=$taskId)"
            "startup_missing_resolution" -> "StartupCoordinator.run() → FAILED(startup_missing_resolution)"
            else -> return null
        }
        return "▶️ FLOW  $message${correlationSuffix(event.correlationId)}"
    }

    private fun correlationSuffix(correlationId: CorrelationId?): String =
        correlationId?.value?.takeIf(String::isNotBlank)?.let { "  [$it]" }.orEmpty()

    private companion object {
        const val STARTUP_CATEGORY = "startup"
    }
}
