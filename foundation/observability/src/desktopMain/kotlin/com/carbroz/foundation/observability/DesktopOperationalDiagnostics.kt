package com.carbroz.foundation.observability

import java.awt.EventQueue

/** Desktop UI-thread heartbeat adapter for Compose Desktop's AWT event queue. */
class DesktopMainThreadDispatcher : MainThreadDispatcher {
    override fun dispatch(block: () -> Unit) {
        EventQueue.invokeLater(block)
    }
}

/** Lightweight Desktop process resource sampling backed by the JVM runtime. */
class DesktopResourceDiagnostics : ResourceDiagnostics {
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
        ResourceDiagnosticResult.Unavailable("desktop runtime resource sampling failed")
    }
}

/** Sanitized console diagnostic sink for Desktop development and operational support. */
class DesktopPlatformDiagnosticSink : LogSink, CrashSink, PerformanceSink, TraceSink, ResponsivenessSink, ResourceSink {
    override fun emit(event: LogEvent) {
        val rendered = render(event.category, event.message, event.correlationId, event.attributes)
        if (event.level.ordinal >= LogLevel.WARN.ordinal) System.err.println(rendered) else println(rendered)
    }

    override fun record(event: CrashEvent, throwable: Throwable?) {
        System.err.println(render(event.category, event.message, event.correlationId, event.attributes))
    }

    override fun record(metric: PerformanceMetric) {
        println("metric=${metric.name} duration_ms=${metric.durationMillis} correlation=${metric.correlationId?.value.orEmpty()} ${renderAttributes(metric.attributes)}")
    }

    override fun record(span: TraceSpan) {
        println("trace=${span.name} duration_ms=${span.durationMillis} outcome=${span.outcome.name.lowercase()} correlation=${span.correlationId.value} ${renderAttributes(span.attributes)}")
    }

    override fun record(incident: ResponsivenessIncident) {
        System.err.println("responsiveness=${incident.scope} blocked_ms=${incident.blockedMillis} threshold_ms=${incident.thresholdMillis}")
    }

    override fun record(snapshot: ResourceSnapshot) {
        println(
            "resources heap_used=${snapshot.heapUsedBytes ?: -1} heap_limit=${snapshot.heapLimitBytes ?: -1} physical_memory=${snapshot.physicalMemoryBytes ?: -1} processors=${snapshot.processorCount ?: -1}",
        )
    }

    private fun render(
        category: String,
        message: String,
        correlationId: CorrelationId?,
        attributes: Map<String, DiagnosticAttribute>,
    ): String = "category=$category event=$message correlation=${correlationId?.value.orEmpty()} ${renderAttributes(attributes)}"

    private fun renderAttributes(attributes: Map<String, DiagnosticAttribute>): String =
        attributes.entries.joinToString(separator = " ") { (key, attribute) -> "$key=${attribute.value}" }
}
