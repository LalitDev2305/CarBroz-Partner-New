package com.carbroz.foundation.observability

/**
 * Vendor-neutral sink bundle supplied by the platform/composition boundary.
 *
 * Production builds may replace any sink with a vendor adapter without changing runtime/data modules.
 */
data class ObservabilitySinks(
    val log: LogSink = LogSink {},
    val crash: CrashSink = CrashSink { _, _ -> },
    val performance: PerformanceSink = PerformanceSink {},
    val trace: TraceSink = TraceSink {},
    val responsiveness: ResponsivenessSink = ResponsivenessSink {},
    val resource: ResourceSink = ResourceSink {},
)
