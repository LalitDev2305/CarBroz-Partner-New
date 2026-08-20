package com.carbroz.partner.core.observability.logger

import com.carbroz.partner.core.observability.model.AttributeSensitivity
import com.carbroz.partner.core.observability.model.LogAttribute
import com.carbroz.partner.core.observability.model.LogCategory
import com.carbroz.partner.core.observability.model.LogEvent
import com.carbroz.partner.core.observability.model.LogLevel
import com.carbroz.partner.core.observability.model.LogValue
import com.carbroz.partner.core.observability.model.TraceContext
import com.carbroz.partner.core.observability.policy.ObservabilityConfig
import com.carbroz.partner.core.observability.redaction.Redactor
import com.carbroz.partner.core.observability.sink.LogSink
import kotlin.time.Clock

/**
 * Default pipeline implementation of [StructuredLogger] enforcing config filtering, security redaction, and isolated sink dispatch.
 */
class DefaultPipelineLogger(
    private val config: ObservabilityConfig = ObservabilityConfig(),
    private val clock: Clock = Clock.System,
    sinks: List<LogSink>
) : StructuredLogger {

    init {
        require(sinks.isNotEmpty()) {
            "DefaultPipelineLogger requires at least one operational LogSink to prevent silent log drop."
        }
    }

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

            // 1. Recursive Sensitivity Policy Filtering
            val filteredAttributes = filterAttributesBySensitivity(attributes)

            // 2. Security Redaction & Error Conversion
            val sanitizedMessage = Redactor.sanitizeText(message) ?: ""
            val sanitizedAttributes = Redactor.sanitizeAttributes(filteredAttributes)
            val errorInfo = Redactor.sanitizeThrowable(throwable)

            // Merge bound defaultTraceContext with operation traceContext
            val mergedContext = defaultTraceContext?.merge(traceContext) ?: traceContext

            // 3. Immutable LogEvent Assembly
            val logEvent = LogEvent(
                timestampMs = clock.now().toEpochMilliseconds(),
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

        private fun filterAttributesBySensitivity(attributes: Map<String, LogAttribute>): Map<String, LogAttribute> {
            if (attributes.isEmpty()) return attributes
            val result = LinkedHashMap<String, LogAttribute>(attributes.size)
            for ((key, attr) in attributes) {
                if (isSensitivityPermitted(attr.sensitivity)) {
                    val filteredValue = filterLogValueSensitivity(attr.value)
                    if (filteredValue != null) {
                        result[key] = LogAttribute(filteredValue, attr.sensitivity)
                    }
                }
            }
            return result
        }

        private fun filterLogValueSensitivity(value: LogValue): LogValue? = when (value) {
            is LogValue.Structure -> {
                val filteredMap = LinkedHashMap<String, LogAttribute>(value.attributes.size)
                for ((k, v) in value.attributes) {
                    if (isSensitivityPermitted(v.sensitivity)) {
                        val subVal = filterLogValueSensitivity(v.value)
                        if (subVal != null) {
                            filteredMap[k] = LogAttribute(subVal, v.sensitivity)
                        }
                    }
                }
                LogValue.Structure(filteredMap)
            }
            is LogValue.Collection -> {
                val filteredList = ArrayList<LogValue>(value.items.size)
                for (item in value.items) {
                    val subVal = filterLogValueSensitivity(item)
                    if (subVal != null) {
                        filteredList.add(subVal)
                    }
                }
                LogValue.Collection(filteredList)
            }
            else -> value
        }

        private fun isSensitivityPermitted(sensitivity: AttributeSensitivity): Boolean = when (sensitivity) {
            AttributeSensitivity.PUBLIC -> true
            AttributeSensitivity.DETAIL -> config.allowDetailedDiagnostics
            AttributeSensitivity.PAYLOAD -> config.allowPayloadLogging
        }
    }
}
