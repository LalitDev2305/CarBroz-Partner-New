package com.carbroz.partner.core.navigation.command

/**
 * Explicit targeting mechanism for PopTo stack operations.
 */
sealed interface PopToTarget {
    data class ByEntryId(val entryId: String) : PopToTarget
    data class ByRoute(val route: String) : PopToTarget
}
