package com.carbroz.partner.core.observability.logger

import com.carbroz.partner.core.observability.model.LogAttribute
import com.carbroz.partner.core.observability.model.LogCategory
import com.carbroz.partner.core.observability.model.LogLevel
import com.carbroz.partner.core.observability.model.TraceContext

/**
 * Ergonomic logger interface with sourceClass identity pre-bound.
 */
interface BoundLogger {

    fun log(
        level: LogLevel,
        category: LogCategory,
        sourceFunction: String,
        event: String,
        message: String,
        attributes: Map<String, LogAttribute> = emptyMap(),
        traceContext: TraceContext? = null,
        durationMs: Long? = null,
        throwable: Throwable? = null
    )

    fun info(
        sourceFunction: String,
        category: LogCategory,
        event: String,
        message: String,
        attributes: Map<String, LogAttribute> = emptyMap(),
        traceContext: TraceContext? = null
    )

    fun debug(
        sourceFunction: String,
        category: LogCategory,
        event: String,
        message: String,
        attributes: Map<String, LogAttribute> = emptyMap(),
        traceContext: TraceContext? = null
    )

    fun error(
        sourceFunction: String,
        category: LogCategory,
        event: String,
        message: String,
        throwable: Throwable? = null,
        attributes: Map<String, LogAttribute> = emptyMap(),
        traceContext: TraceContext? = null
    )
}
