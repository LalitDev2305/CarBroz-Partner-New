package com.carbroz.partner.core.navigation.state

import com.carbroz.partner.core.navigation.stack.NavEntry
import com.carbroz.partner.core.navigation.stack.NavStack

/**
 * Immutable state container exposing active navigation stack state.
 */
data class NavState(
    val stack: NavStack
) {
    val activeEntry: NavEntry
        get() = stack.current
}
