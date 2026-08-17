package com.carbroz.partner.core.observability.sink

import com.carbroz.partner.core.observability.model.LogEvent

/**
 * Functional strategy contract for log event destinations.
 */
fun interface LogSink {
    /**
     * Dispatch a sanitized, immutable [LogEvent] to this destination.
     */
    fun send(event: LogEvent)
}
