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
    val correlationId: CorrelationId? = null,
    val attributes: Map<String, DiagnosticAttribute> = emptyMap(),
)

/** Crash/fatal diagnostic report. Throwable details remain behind the platform/vendor sink boundary. */
data class CrashEvent(
    val category: String,
    val message: String,
    val correlationId: CorrelationId? = null,
    val attributes: Map<String, DiagnosticAttribute> = emptyMap(),
)

/** Completed performance measurement in milliseconds. */
data class PerformanceMetric(
    val name: String,
    val durationMillis: Long,
    val correlationId: CorrelationId? = null,
    val attributes: Map<String, DiagnosticAttribute> = emptyMap(),
) {
    init {
        require(durationMillis >= 0) { "Performance duration must be non-negative." }
    }
}

/** Receives sanitized operational logs. Implementations must not be called directly by product code. */
fun interface LogSink { fun emit(event: LogEvent) }

/** Receives sanitized crash reports. Throwable forwarding is disabled unless explicitly allowed by policy. */
fun interface CrashSink { fun record(event: CrashEvent, throwable: Throwable?) }

/** Receives sanitized performance measurements. */
fun interface PerformanceSink { fun record(metric: PerformanceMetric) }

/** Receives bounded platform resource snapshots. */
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

/**
 * Canonical product-neutral operational diagnostics facade.
 *
 * Every sink invocation is fail-isolated so diagnostics can never change application control flow.
 * Data is bounded and sensitive attributes are redacted before crossing a sink boundary.
 */
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
        runCatching { logSink.emit(event.sanitized(policy.maxAttributes)) }
    }

    fun crash(event: CrashEvent, throwable: Throwable? = null) {
        if (!policy.crashReportingEnabled) return
        runCatching {
            crashSink.record(
                event = event.sanitized(policy.maxAttributes),
                throwable = throwable.takeIf { policy.includeThrowableDetails },
            )
        }
    }

    fun performance(metric: PerformanceMetric) {
        if (!policy.performanceMetricsEnabled) return
        runCatching { performanceSink.record(metric.sanitized(policy.maxAttributes)) }
    }

    fun trace(span: TraceSpan) {
        if (!policy.tracingEnabled) return
        runCatching { traceSink.record(span.sanitized(policy.maxAttributes)) }
    }

    fun responsiveness(incident: ResponsivenessIncident) {
        if (!policy.responsivenessReportingEnabled) return
        runCatching { responsivenessSink.record(incident) }
    }

    fun resource(snapshot: ResourceSnapshot) {
        if (!policy.resourceDiagnosticsEnabled) return
        runCatching { resourceSink.record(snapshot) }
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
private const val MAX_CATEGORY_LENGTH = 80
private const val MAX_MESSAGE_LENGTH = 160
private const val MAX_METRIC_NAME_LENGTH = 120
private const val MAX_ATTRIBUTE_KEY_LENGTH = 80
private const val MAX_ATTRIBUTE_VALUE_LENGTH = 512

private fun Map<String, DiagnosticAttribute>.sanitized(maxAttributes: Int): Map<String, DiagnosticAttribute> =
    entries.take(maxAttributes).associate { (key, attribute) ->
        key.take(MAX_ATTRIBUTE_KEY_LENGTH) to if (attribute.sensitivity == DiagnosticSensitivity.SENSITIVE) {
            DiagnosticAttribute(REDACTED)
        } else {
            attribute.copy(value = attribute.value.take(MAX_ATTRIBUTE_VALUE_LENGTH))
        }
    }

private fun LogEvent.sanitized(maxAttributes: Int) = copy(
    category = category.take(MAX_CATEGORY_LENGTH),
    message = message.take(MAX_MESSAGE_LENGTH),
    attributes = attributes.sanitized(maxAttributes),
)

private fun CrashEvent.sanitized(maxAttributes: Int) = copy(
    category = category.take(MAX_CATEGORY_LENGTH),
    message = message.take(MAX_MESSAGE_LENGTH),
    attributes = attributes.sanitized(maxAttributes),
)

private fun PerformanceMetric.sanitized(maxAttributes: Int) = copy(
    name = name.take(MAX_METRIC_NAME_LENGTH),
    attributes = attributes.sanitized(maxAttributes),
)

private fun TraceSpan.sanitized(maxAttributes: Int) = copy(attributes = attributes.sanitized(maxAttributes))
