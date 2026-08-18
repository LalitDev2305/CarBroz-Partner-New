package com.carbroz.partner.domain.actions.binding

import kotlin.jvm.JvmInline

/**
 * Pure declarative representation of a runtime binding expression (e.g. "${session.partnerId}").
 */
@JvmInline
value class BindingExpression(val rawExpression: String) {
    init {
        require(rawExpression.isNotBlank()) { "BindingExpression rawExpression must not be blank" }
        val trimmed = rawExpression.trim()
        require(trimmed.startsWith("\${") && trimmed.endsWith("}")) {
            "BindingExpression must start with '\${' and end with '}'"
        }
        val inner = trimmed.substring(2, trimmed.length - 1).trim()
        require(inner.isNotEmpty()) { "BindingExpression inner expression must not be empty" }
    }
}
