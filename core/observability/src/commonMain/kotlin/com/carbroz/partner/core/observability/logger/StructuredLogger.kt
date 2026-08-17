package com.carbroz.partner.core.observability.logger

import com.carbroz.partner.core.observability.model.LogLevel
import com.carbroz.partner.core.observability.model.TraceContext


/**
 * Primary entry boundary for creating bound loggers and evaluating log level enablement.
 */
interface StructuredLogger {

    /**
     * Create a [BoundLogger] instance with the specified source class name and optional default context pre-bound.
     */
    fun withSource(sourceClass: String, defaultTraceContext: TraceContext? = null): BoundLogger


    /**
     * Check if the specified log level is enabled under the current configuration.
     */
    fun isLevelEnabled(level: LogLevel): Boolean
}
