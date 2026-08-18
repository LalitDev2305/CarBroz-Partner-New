package com.carbroz.partner.domain.actions.model

import kotlin.jvm.JvmInline

/**
 * Immutable value object representing a unique runtime instance identifier for an action.
 */
@JvmInline
value class ActionId(val value: String) {
    init {
        require(value.isNotBlank()) { "ActionId must not be blank or empty" }
    }
}
