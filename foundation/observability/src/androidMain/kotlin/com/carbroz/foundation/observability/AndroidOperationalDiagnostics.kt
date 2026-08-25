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
 * Privacy-safe native diagnostic sink. Events have already been policy-filtered and redacted by [Observability].
 * Throwable text is intentionally not rendered here because exception messages may contain sensitive values.
 */
class AndroidPlatformDiagnosticSink(
    private val tag: String = "CarBroz",
) : LogSink, CrashSink, PerformanceSink, TraceSink, ResponsivenessSink, ResourceSink {
    override fun emit(event: LogEvent) {
        val message = render(event.category, event.message, event.correlationId, event.attributes)
        when (event.level) {
            LogLevel.DEBUG -> Log.d(tag, message)
            LogLevel.INFO -> Log.i(tag, message)
            LogLevel.WARN -> Log.w(tag, message)
            LogLevel.ERROR -> Log.e(tag, message)
        }
    }

    override fun record(event: CrashEvent, throwable: Throwable?) {
        Log.e(tag, render(event.category, event.message, event.correlationId, event.attributes))
    }

    override fun record(metric: PerformanceMetric) {
        Log.d(tag, "metric=${metric.name} duration_ms=${metric.durationMillis} correlation=${metric.correlationId.orEmpty()} ${renderAttributes(metric.attributes)}")
    }

    override fun record(span: TraceSpan) {
        Log.d(tag, "trace=${span.name} duration_ms=${span.durationMillis} outcome=${span.outcome.name.lowercase()} correlation=${span.correlationId.value} ${renderAttributes(span.attributes)}")
    }

    override fun record(incident: ResponsivenessIncident) {
        Log.w(tag, "responsiveness=${incident.scope} blocked_ms=${incident.blockedMillis} threshold_ms=${incident.thresholdMillis}")
    }

    override fun record(snapshot: ResourceSnapshot) {
        Log.d(tag, "resources heap_used=${snapshot.heapUsedBytes ?: -1} heap_limit=${snapshot.heapLimitBytes ?: -1} processors=${snapshot.processorCount ?: -1}")
    }

    private fun render(
        category: String,
        message: String,
        correlationId: String?,
        attributes: Map<String, DiagnosticAttribute>,
    ): String = "category=$category event=$message correlation=${correlationId.orEmpty()} ${renderAttributes(attributes)}"

    private fun renderAttributes(attributes: Map<String, DiagnosticAttribute>): String =
        attributes.entries.joinToString(separator = " ") { (key, attribute) -> "$key=${attribute.value}" }
}
