package com.carbroz.foundation.observability

import kotlin.random.Random

/** Stable opaque correlation identifier used to connect logs, traces and metrics for one logical operation. */
@kotlin.jvm.JvmInline
value class CorrelationId(val value: String) {
    init {
        require(value.isNotBlank()) { "CorrelationId must not be blank." }
        require(value.length <= MAX_LENGTH) { "CorrelationId must be <= $MAX_LENGTH characters." }
    }

    companion object {
        const val MAX_LENGTH: Int = 128
    }
}

/** Supplies non-sensitive opaque identifiers without imposing a vendor tracing SDK. */
fun interface CorrelationIdProvider {
    fun next(scope: String): CorrelationId
}

/**
 * Default process correlation-id source.
 *
 * IDs intentionally contain no user, session, device or business identifiers. Randomness is used for
 * collision resistance only; correlation IDs are not security credentials.
 */
class RandomCorrelationIdProvider(
    private val random: Random = Random.Default,
) : CorrelationIdProvider {
    override fun next(scope: String): CorrelationId {
        val normalizedScope = scope
            .lowercase()
            .filter { it.isLetterOrDigit() || it == '-' || it == '_' }
            .take(MAX_SCOPE_LENGTH)
            .ifBlank { DEFAULT_SCOPE }
        val randomPart = random.nextLong().toULong().toString(radix = 16).padStart(16, '0')
        return CorrelationId("$normalizedScope-$randomPart")
    }

    private companion object {
        const val MAX_SCOPE_LENGTH = 24
        const val DEFAULT_SCOPE = "operation"
    }
}

/** Final outcome for one traced operation span. Retry is distinct from terminal failure. */
enum class TraceOutcome { SUCCESS, FAILURE, RETRY, CANCELLED }

/** Completed trace span. Parent correlation is optional for nested operations. */
data class TraceSpan(
    val name: String,
    val correlationId: CorrelationId,
    val parentCorrelationId: CorrelationId? = null,
    val durationMillis: Long,
    val outcome: TraceOutcome,
    val attributes: Map<String, DiagnosticAttribute> = emptyMap(),
) {
    init {
        require(name.isNotBlank()) { "Trace name must not be blank." }
        require(name.length <= 120) { "Trace name must be <= 120 characters." }
        require(durationMillis >= 0) { "Trace duration must be non-negative." }
    }
}

/** Main/UI responsiveness degradation detected by a platform watchdog or OS diagnostic callback. */
data class ResponsivenessIncident(
    val scope: String,
    val blockedMillis: Long,
    val thresholdMillis: Long,
    val correlationId: CorrelationId? = null,
) {
    init {
        require(scope.isNotBlank()) { "Responsiveness scope must not be blank." }
        require(blockedMillis >= 0) { "blockedMillis must be non-negative." }
        require(thresholdMillis > 0) { "thresholdMillis must be positive." }
    }
}

/** Platform-neutral snapshot of process resources. Unknown measurements remain null rather than guessed. */
data class ResourceSnapshot(
    val heapUsedBytes: Long? = null,
    val heapLimitBytes: Long? = null,
    val physicalMemoryBytes: Long? = null,
    val processorCount: Int? = null,
) {
    init {
        require(heapUsedBytes == null || heapUsedBytes >= 0)
        require(heapLimitBytes == null || heapLimitBytes >= 0)
        require(physicalMemoryBytes == null || physicalMemoryBytes >= 0)
        require(processorCount == null || processorCount > 0)
    }
}

/** Explicit result because some platforms cannot expose every process resource safely or portably. */
sealed interface ResourceDiagnosticResult {
    data class Available(val snapshot: ResourceSnapshot) : ResourceDiagnosticResult
    data class Unsupported(val reason: String) : ResourceDiagnosticResult
    data class Unavailable(val reason: String) : ResourceDiagnosticResult
}

/** Platform adapter for bounded process-resource sampling. Sampling must never block application work. */
fun interface ResourceDiagnostics {
    fun sample(): ResourceDiagnosticResult
}

/** Receives sanitized completed trace spans. */
fun interface TraceSink {
    fun record(span: TraceSpan)
}

/** Receives typed main/UI responsiveness incidents. */
fun interface ResponsivenessSink {
    fun record(incident: ResponsivenessIncident)
}
