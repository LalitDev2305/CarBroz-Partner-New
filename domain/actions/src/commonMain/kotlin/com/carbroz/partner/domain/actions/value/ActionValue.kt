package com.carbroz.partner.domain.actions.value

import com.carbroz.partner.domain.actions.binding.BindingExpression

/**
 * Type-safe, immutable domain value tree for dynamic action parameters.
 *
 * Prevents dynamic Any/Map<String, Any> usage and preserves exact decimal string precision.
 */
sealed interface ActionValue {
    data class Text(val value: String) : ActionValue
    data class Integer(val value: Long) : ActionValue
    
    data class Decimal(val value: String) : ActionValue {
        init {
            require(value.isNotBlank()) { "Decimal string value must not be blank" }
            val trimmed = value.trim()
            require(DECIMAL_REGEX.matches(trimmed)) { "Invalid decimal format: '$value'" }
        }

        companion object {
            private val DECIMAL_REGEX = Regex("^-?\\d+(\\.\\d+)?$")
        }
    }

    data class Flag(val value: Boolean) : ActionValue
    data class Binding(val expression: BindingExpression) : ActionValue

    class Object private constructor(
        properties: Map<String, ActionValue>
    ) : ActionValue {
        val properties: Map<String, ActionValue> = properties.toMap()

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is Object) return false
            return properties == other.properties
        }

        override fun hashCode(): Int = properties.hashCode()
        override fun toString(): String = "Object(size=${properties.size}, keys=${properties.keys})"

        companion object {
            fun create(properties: Map<String, ActionValue> = emptyMap()): Object = Object(properties)
        }
    }

    class List private constructor(
        items: kotlin.collections.List<ActionValue>
    ) : ActionValue {
        val items: kotlin.collections.List<ActionValue> = items.toList()

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is List) return false
            return items == other.items
        }

        override fun hashCode(): Int = items.hashCode()
        override fun toString(): String = "List(size=${items.size})"

        companion object {
            fun create(items: kotlin.collections.List<ActionValue> = emptyList()): List = List(items)
        }
    }

    data object Null : ActionValue
}
