package com.carbroz.partner.core.observability.logger

import com.carbroz.partner.core.observability.model.AttributeSensitivity
import com.carbroz.partner.core.observability.model.Clock
import com.carbroz.partner.core.observability.model.LogAttribute
import com.carbroz.partner.core.observability.model.LogCategory
import com.carbroz.partner.core.observability.model.LogEvent
import com.carbroz.partner.core.observability.model.LogLevel
import com.carbroz.partner.core.observability.model.TraceContext
import com.carbroz.partner.core.observability.policy.ObservabilityConfig
import com.carbroz.partner.core.observability.redaction.Redactor
import com.carbroz.partner.core.observability.sink.LogSink

/**
 * Default pipeline implementation of [StructuredLogger] enforcing config filtering, security redaction, and isolated sink dispatch.
 */
class DefaultPipelineLogger(
    private val config: ObservabilityConfig = ObservabilityConfig(),
    private val clock: Clock = Clock { 0L },
    sinks: List<LogSink> = emptyList()
) : StructuredLogger {

    private val sinkSnapshot: List<LogSink> = sinks.toList()


    override fun isLevelEnabled(level: LogLevel): Boolean {
        return config.isEnabled && level.ordinal >= config.minLevel.ordinal
    }

    override fun withSource(sourceClass: String, defaultTraceContext: TraceContext?): BoundLogger {
        return InternalBoundLogger(sourceClass, defaultTraceContext)
    }

    private inner class InternalBoundLogger(
        private val sourceClass: String,
        private val defaultTraceContext: TraceContext? = null
    ) : BoundLogger {

        override fun log(
            level: LogLevel,
            category: LogCategory,
            sourceFunction: String,
            event: String,
            message: String,
            attributes: Map<String, LogAttribute>,
            traceContext: TraceContext?,
            durationMs: Long?,
            throwable: Throwable?
        ) {
            if (!isLevelEnabled(level) || !config.enabledCategories.contains(category) || sinkSnapshot.isEmpty()) {
                return
            }

            // 1. Sensitivity Policy Filtering
            val filteredAttributes = filterAttributesBySensitivity(attributes)

            // 2. Security Redaction & Error Conversion
            val sanitizedMessage = Redactor.sanitizeText(message) ?: ""
            val sanitizedAttributes = Redactor.sanitizeAttributes(filteredAttributes)
            val errorInfo = Redactor.sanitizeThrowable(throwable)

            // Merge bound defaultTraceContext with operation traceContext
            val mergedContext = defaultTraceContext?.merge(traceContext) ?: traceContext

            // 3. Immutable LogEvent Assembly
            val logEvent = LogEvent(
                timestampMs = clock.nowEpochMilliseconds(),
                level = level,
                category = category,
                sourceClass = sourceClass,
                sourceFunction = sourceFunction,
                event = event,
                message = sanitizedMessage,
                attributes = sanitizedAttributes,
                traceContext = mergedContext,
                durationMs = if (config.enablePerformanceTiming) durationMs else null,
                errorInfo = errorInfo
            )


            // 4. Isolated Multi-Sink Dispatch
            for (sink in sinkSnapshot) {
                try {
                    sink.send(logEvent)
                } catch (_: Throwable) {
                    // Sink failures are isolated to prevent application crashes or recursive failure loops
                }
            }

        }

        override fun info(
            sourceFunction: String,
            category: LogCategory,
            event: String,
            message: String,
            attributes: Map<String, LogAttribute>,
            traceContext: TraceContext?
        ) {
            log(
                level = LogLevel.INFO,
                category = category,
                sourceFunction = sourceFunction,
                event = event,
                message = message,
                attributes = attributes,
                traceContext = traceContext
            )
        }

        override fun debug(
            sourceFunction: String,
            category: LogCategory,
            event: String,
            message: String,
            attributes: Map<String, LogAttribute>,
            traceContext: TraceContext?
        ) {
            log(
                level = LogLevel.DEBUG,
                category = category,
                sourceFunction = sourceFunction,
                event = event,
                message = message,
                attributes = attributes,
                traceContext = traceContext
            )
        }

        override fun error(
            sourceFunction: String,
            category: LogCategory,
            event: String,
            message: String,
            throwable: Throwable?,
            attributes: Map<String, LogAttribute>,
            traceContext: TraceContext?
        ) {
            log(
                level = LogLevel.ERROR,
                category = category,
                sourceFunction = sourceFunction,
                event = event,
                message = message,
                attributes = attributes,
                traceContext = traceContext,
                throwable = throwable
            )
        }

        private fun filterAttributesBySensitivity(attributes: Map<String, LogAttribute>): Map<String, LogAttribute> {
            if (attributes.isEmpty()) return attributes
            val result = LinkedHashMap<String, LogAttribute>(attributes.size)
            for ((key, attr) in attributes) {
                when (attr.sensitivity) {
                    AttributeSensitivity.PUBLIC -> result[key] = attr
                    AttributeSensitivity.DETAIL -> if (config.allowDetailedDiagnostics) result[key] = attr
                    AttributeSensitivity.PAYLOAD -> if (config.allowPayloadLogging) result[key] = attr
                }
            }
            return result
        }
    }
}
