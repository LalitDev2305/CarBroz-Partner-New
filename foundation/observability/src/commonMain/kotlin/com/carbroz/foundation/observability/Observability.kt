package com.carbroz.foundation.observability

/** Severity for operational diagnostic events. */
enum class LogLevel { DEBUG, INFO, WARN, ERROR }

/** Classification used to prevent sensitive diagnostic values from being emitted verbatim. */
enum class DiagnosticSensitivity { PUBLIC, SENSITIVE }

/** Typed diagnostic attribute. Sensitive values are always redacted before reaching sinks. */
data class DiagnosticAttribute(
    val value: String,
    val sensitivity: DiagnosticSensitivity = DiagnosticSensitivity.PUBLIC,
)

/** Privacy-safe operational log event. */
data class LogEvent(
    val level: LogLevel,
    val category: String,
    val message: String,
    val correlationId: String? = null,
    val attributes: Map<String, DiagnosticAttribute> = emptyMap(),
)

/** Crash/fatal diagnostic report. Throwable details remain behind the platform/vendor sink boundary. */
data class CrashEvent(
    val category: String,
    val message: String,
    val correlationId: String? = null,
    val attributes: Map<String, DiagnosticAttribute> = emptyMap(),
)

/** Completed performance measurement in milliseconds. */
data class PerformanceMetric(
    val name: String,
    val durationMillis: Long,
    val correlationId: String? = null,
    val attributes: Map<String, DiagnosticAttribute> = emptyMap(),
) {
    init {
        require(durationMillis >= 0) { "Performance duration must be non-negative." }
    }
}

fun interface LogSink { fun emit(event: LogEvent) }
fun interface CrashSink { fun record(event: CrashEvent, throwable: Throwable?) }
fun interface PerformanceSink { fun record(metric: PerformanceMetric) }
fun interface ResourceSink { fun record(snapshot: ResourceSnapshot) }

/** Runtime policy controlling operational diagnostics without leaking environment branching into callers. */
data class ObservabilityPolicy(
    val minimumLogLevel: LogLevel = LogLevel.INFO,
    val crashReportingEnabled: Boolean = true,
    val performanceMetricsEnabled: Boolean = true,
    val tracingEnabled: Boolean = true,
    val responsivenessReportingEnabled: Boolean = true,
    val resourceDiagnosticsEnabled: Boolean = true,
    val includeThrowableDetails: Boolean = false,
    val maxAttributes: Int = 32,
) {
    init { require(maxAttributes in 0..128) }
}

/** Canonical product-neutral operational diagnostics facade. */
class Observability(
    private val policy: ObservabilityPolicy,
    private val logSink: LogSink = LogSink {},
    private val crashSink: CrashSink = CrashSink { _, _ -> },
    private val performanceSink: PerformanceSink = PerformanceSink {},
    private val traceSink: TraceSink = TraceSink {},
    private val responsivenessSink: ResponsivenessSink = ResponsivenessSink {},
    private val resourceSink: ResourceSink = ResourceSink {},
) {
    fun log(event: LogEvent) {
        if (event.level.ordinal < policy.minimumLogLevel.ordinal) return
        logSink.emit(event.sanitized(policy.maxAttributes))
    }

    fun crash(event: CrashEvent, throwable: Throwable? = null) {
        if (!policy.crashReportingEnabled) return
        crashSink.record(
            event = event.sanitized(policy.maxAttributes),
            throwable = throwable.takeIf { policy.includeThrowableDetails },
        )
    }

    fun performance(metric: PerformanceMetric) {
        if (!policy.performanceMetricsEnabled) return
        performanceSink.record(metric.sanitized(policy.maxAttributes))
    }

    fun trace(span: TraceSpan) {
        if (!policy.tracingEnabled) return
        traceSink.record(span.sanitized(policy.maxAttributes))
    }

    fun responsiveness(incident: ResponsivenessIncident) {
        if (!policy.responsivenessReportingEnabled) return
        responsivenessSink.record(incident)
    }

    fun resource(snapshot: ResourceSnapshot) {
        if (!policy.resourceDiagnosticsEnabled) return
        resourceSink.record(snapshot)
    }
}

/** Shared no-op instance used as a source-compatible default by instrumented subsystems. */
val NoOpObservability: Observability = Observability(
    policy = ObservabilityPolicy(
        minimumLogLevel = LogLevel.ERROR,
        crashReportingEnabled = false,
        performanceMetricsEnabled = false,
        tracingEnabled = false,
        responsivenessReportingEnabled = false,
        resourceDiagnosticsEnabled = false,
        maxAttributes = 0,
    ),
)

private const val REDACTED = "[REDACTED]"

private fun Map<String, DiagnosticAttribute>.sanitized(maxAttributes: Int): Map<String, DiagnosticAttribute> =
    entries.take(maxAttributes).associate { (key, attribute) ->
        key to if (attribute.sensitivity == DiagnosticSensitivity.SENSITIVE) {
            DiagnosticAttribute(REDACTED)
        } else {
            attribute
        }
    }

private fun LogEvent.sanitized(maxAttributes: Int) = copy(attributes = attributes.sanitized(maxAttributes))
private fun CrashEvent.sanitized(maxAttributes: Int) = copy(attributes = attributes.sanitized(maxAttributes))
private fun PerformanceMetric.sanitized(maxAttributes: Int) = copy(attributes = attributes.sanitized(maxAttributes))
private fun TraceSpan.sanitized(maxAttributes: Int) = copy(attributes = attributes.sanitized(maxAttributes))
