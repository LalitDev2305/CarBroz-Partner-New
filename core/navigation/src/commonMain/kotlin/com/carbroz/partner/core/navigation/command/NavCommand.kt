package com.carbroz.partner.core.navigation.command

import com.carbroz.partner.core.navigation.destination.NavDestination

/**
 * Sealed interface representing atomic navigation operations.
 */
sealed interface NavCommand {
    data class Push(val destination: NavDestination) : NavCommand
    data class Replace(val destination: NavDestination) : NavCommand
    data object Pop : NavCommand
    data class PopTo(val target: PopToTarget, val inclusive: Boolean = false) : NavCommand
    data class ResetTo(val destination: NavDestination) : NavCommand
}
