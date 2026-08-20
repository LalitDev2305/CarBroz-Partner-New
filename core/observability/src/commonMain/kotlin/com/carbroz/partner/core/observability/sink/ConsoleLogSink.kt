package com.carbroz.partner.core.observability.sink

import com.carbroz.partner.core.observability.model.LogEvent

/**
 * Standard Multiplatform System Console (stdout/stderr) [LogSink] implementation.
 * Outputs formatted, sanitized log events to standard system output streams.
 */
public class ConsoleLogSink : LogSink {
    override fun send(event: LogEvent) {
        println("[${event.level.name}][${event.category.name}][${event.sourceClass}::${event.sourceFunction}] ${event.event}: ${event.message}")
    }
}
