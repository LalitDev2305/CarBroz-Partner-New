package com.carbroz.partner.core.observability.model

/**
 * Immutable final diagnostic log event dispatched to sinks.
 */
data class LogEvent(
    val timestampMs: Long,
    val level: LogLevel,
    val category: LogCategory,
    val sourceClass: String,
    val sourceFunction: String,
    val event: String,
    val message: String,
    val attributes: Map<String, LogValue> = emptyMap(),
    val traceContext: TraceContext? = null,
    val durationMs: Long? = null,
    val errorInfo: ErrorInfo? = null
)
