package com.carbroz.partner.engine.execution.action

import com.carbroz.partner.engine.execution.binding.BindingExpression

/**
 * Type-safe, immutable domain value tree for dynamic action parameters.
 *
 * Prevents dynamic Any/Map<String, Any> usage and preserves exact decimal string precision.
 */
public sealed interface ActionValue {
    public data class Text(val value: String) : ActionValue
    public data class Integer(val value: Long) : ActionValue

    public data class Decimal(val value: String) : ActionValue {
        init {
            require(value.isNotBlank()) { "Decimal string value must not be blank" }
            val trimmed = value.trim()
            require(DECIMAL_REGEX.matches(trimmed)) { "Invalid decimal format: '$value'" }
        }

        private companion object {
            private val DECIMAL_REGEX = Regex("^-?\\d+(\\.\\d+)?$")
        }
    }

    public data class Flag(val value: Boolean) : ActionValue
    public data class Binding(val expression: BindingExpression) : ActionValue

    public class Object private constructor(
        properties: Map<String, ActionValue>
    ) : ActionValue {
        public val properties: Map<String, ActionValue> = properties.toMap()

        init {
            require(properties.keys.all { it.isNotBlank() }) { "Object property keys must not be blank" }
        }

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is Object) return false
            return properties == other.properties
        }

        override fun hashCode(): Int = properties.hashCode()
        override fun toString(): String = "Object(size=${properties.size})"

        public companion object {
            public fun create(properties: Map<String, ActionValue> = emptyMap()): Object = Object(properties)
        }
    }

    public class List private constructor(
        items: kotlin.collections.List<ActionValue>
    ) : ActionValue {
        public val items: kotlin.collections.List<ActionValue> = items.toList()

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is List) return false
            return items == other.items
        }

        override fun hashCode(): Int = items.hashCode()
        override fun toString(): String = "List(size=${items.size})"

        public companion object {
            public fun create(items: kotlin.collections.List<ActionValue> = emptyList()): List = List(items)
        }
    }

    public data object Null : ActionValue
}
