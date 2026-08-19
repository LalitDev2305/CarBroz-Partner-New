package com.carbroz.partner.domain.actions.model

import kotlin.jvm.JvmInline

/**
 * Open backend-extensible action type representation wrapping raw action identifier strings.
 */
@JvmInline
value class ActionType(val rawValue: String) {
    init {
        require(rawValue.isNotBlank()) { "ActionType rawValue must not be blank or empty" }
    }

    companion object {
        val NAVIGATION_PUSH = ActionType("navigation.push")
        val NAVIGATION_POP = ActionType("navigation.pop")
        val API_REQUEST = ActionType("api.request")
        val FORM_SUBMIT = ActionType("form.submit")
        val CAPABILITY_EXECUTE = ActionType("capability.execute")
        val AUTH_LOGOUT = ActionType("auth.logout")
    }
}
