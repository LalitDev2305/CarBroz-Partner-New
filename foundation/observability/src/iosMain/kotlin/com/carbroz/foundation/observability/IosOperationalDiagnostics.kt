@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package com.carbroz.foundation.observability

import platform.Foundation.NSProcessInfo
import platform.darwin.dispatch_async
import platform.darwin.dispatch_get_main_queue

/** iOS main-queue heartbeat adapter used by the common responsiveness watchdog. */
class IosMainThreadDispatcher : MainThreadDispatcher {
    override fun dispatch(block: () -> Unit) {
        dispatch_async(dispatch_get_main_queue()) { block() }
    }
}

/**
 * iOS resource sampling exposes only portable process information available without private APIs.
 * Current heap usage is intentionally left unknown rather than inferred from system-wide memory.
 */
class IosResourceDiagnostics : ResourceDiagnostics {
    override fun sample(): ResourceDiagnosticResult = runCatching {
        val processInfo = NSProcessInfo.processInfo
        ResourceDiagnosticResult.Available(
            ResourceSnapshot(
                heapUsedBytes = null,
                heapLimitBytes = processInfo.physicalMemory.toLong().coerceAtLeast(0L),
                processorCount = processInfo.processorCount.toInt().coerceAtLeast(1),
            ),
        )
    }.getOrElse {
        ResourceDiagnosticResult.Unavailable("ios process resource sampling failed")
    }
}

/** Sanitized console diagnostic sink; vendor crash/performance SDKs can replace these sink interfaces at composition. */
class IosPlatformDiagnosticSink : LogSink, CrashSink, PerformanceSink, TraceSink, ResponsivenessSink, ResourceSink {
    override fun emit(event: LogEvent) {
        println(render(event.category, event.message, event.correlationId, event.attributes))
    }

    override fun record(event: CrashEvent, throwable: Throwable?) {
        println(render(event.category, event.message, event.correlationId, event.attributes))
    }

    override fun record(metric: PerformanceMetric) {
        println("metric=${metric.name} duration_ms=${metric.durationMillis} correlation=${metric.correlationId.orEmpty()} ${renderAttributes(metric.attributes)}")
    }

    override fun record(span: TraceSpan) {
        println("trace=${span.name} duration_ms=${span.durationMillis} outcome=${span.outcome.name.lowercase()} correlation=${span.correlationId.value} ${renderAttributes(span.attributes)}")
    }

    override fun record(incident: ResponsivenessIncident) {
        println("responsiveness=${incident.scope} blocked_ms=${incident.blockedMillis} threshold_ms=${incident.thresholdMillis}")
    }

    override fun record(snapshot: ResourceSnapshot) {
        println("resources heap_used=${snapshot.heapUsedBytes ?: -1} heap_limit=${snapshot.heapLimitBytes ?: -1} processors=${snapshot.processorCount ?: -1}")
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
