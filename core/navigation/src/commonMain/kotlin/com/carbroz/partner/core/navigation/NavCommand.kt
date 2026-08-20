package com.carbroz.partner.core.navigation

/**
 * Explicit targeting mechanism for PopTo stack operations.
 */
sealed interface PopToTarget {
    data class ByEntryId(val entryId: String) : PopToTarget {
        init {
            require(entryId.isNotBlank()) { "PopToTarget.ByEntryId entryId must not be blank" }
        }
    }
    data class ByRoute(val route: String) : PopToTarget {
        init {
            require(route.isNotBlank()) { "PopToTarget.ByRoute route must not be blank" }
        }
    }
}

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
