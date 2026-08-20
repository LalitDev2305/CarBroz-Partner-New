package com.carbroz.partner.core.observability.sink

import com.carbroz.partner.core.observability.model.LogEvent

/**
 * Standard Multiplatform System Console (stdout/stderr) [LogSink] implementation.
 * Outputs formatted, sanitized log events to standard system output streams.
 */
public class ConsoleLogSink : LogSink {
    override fun send(event: LogEvent) {
        println(formatLogEvent(event))
    }
}

internal fun formatLogEvent(event: LogEvent): String {
    val builder = StringBuilder()
    builder.append("[${event.timestampMs}]")
    builder.append("[${event.level.name}]")
    builder.append("[${event.category.name}]")
    builder.append("[${event.sourceClass}::${event.sourceFunction}] ")
    builder.append("${event.event}: ${event.message}")

    if (event.attributes.isNotEmpty()) {
        builder.append(" | attributes=").append(event.attributes)
    }
    if (event.traceContext != null) {
        builder.append(" | traceContext=").append(event.traceContext)
    }
    if (event.durationMs != null) {
        builder.append(" | durationMs=").append(event.durationMs)
    }
    if (event.errorInfo != null) {
        builder.append(" | errorInfo=").append(event.errorInfo)
    }
    return builder.toString()
}
