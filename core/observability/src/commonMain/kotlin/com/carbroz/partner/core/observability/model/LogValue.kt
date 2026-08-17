package com.carbroz.partner.core.observability.model

/**
 * Type-safe structured value model avoiding raw `Any?` or `Map<String, Any?>`.
 */
sealed interface LogValue {
    data class Text(val value: String) : LogValue
    data class Integer(val value: Long) : LogValue
    data class Decimal(val value: Double) : LogValue
    data class Flag(val value: Boolean) : LogValue
    data object Null : LogValue
    data class Structure(val attributes: Map<String, LogAttribute>) : LogValue
    data class Collection(val items: List<LogValue>) : LogValue
}

/**
 * Envelope pairing a [LogValue] with its [AttributeSensitivity].
 */
data class LogAttribute(
    val value: LogValue,
    val sensitivity: AttributeSensitivity = AttributeSensitivity.PUBLIC
)
