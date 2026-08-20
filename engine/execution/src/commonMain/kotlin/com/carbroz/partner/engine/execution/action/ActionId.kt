package com.carbroz.partner.engine.execution.action

import kotlin.jvm.JvmInline

/**
 * Immutable value object representing a unique runtime instance identifier for an action.
 */
@JvmInline
public value class ActionId(public val value: String) {
    init {
        require(value.isNotBlank()) { "ActionId must not be blank or empty" }
    }
}
