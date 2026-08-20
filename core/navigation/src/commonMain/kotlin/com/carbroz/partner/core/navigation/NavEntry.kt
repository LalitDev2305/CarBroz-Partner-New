package com.carbroz.partner.core.navigation

/**
 * Pairs a unique runtime [entryId] with a [NavDestination].
 *
 * Allows multiple instances of the same destination route to exist distinctly on the back stack.
 */
data class NavEntry(
    val entryId: String,
    val destination: NavDestination
) {
    init {
        require(entryId.isNotBlank()) { "NavEntry entryId must not be blank" }
    }
}
