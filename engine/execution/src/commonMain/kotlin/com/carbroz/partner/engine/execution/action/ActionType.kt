package com.carbroz.partner.engine.execution.action

import kotlin.jvm.JvmInline

/**
 * Open backend-extensible action type representation wrapping raw action identifier strings.
 */
@JvmInline
public value class ActionType(public val rawValue: String) {
    init {
        require(rawValue.isNotBlank()) { "ActionType rawValue must not be blank or empty" }
    }

    public companion object {
        public val API_REQUEST: ActionType = ActionType("api.request")
        public val AUTH_LOGOUT: ActionType = ActionType("auth.logout")
    }
}
