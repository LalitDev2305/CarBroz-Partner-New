package com.carbroz.foundation.navigation

/**
 * Product-neutral semantic destination understood by application navigation.
 *
 * Concrete destination types live with the subsystem that owns their meaning.
 * They must not implement Navigation 3 framework interfaces; the navigation
 * adapter translates them at the presentation boundary.
 */
interface NavigationDestination {
    /** Stable semantic identity used for equality-sensitive navigation policies. */
    val navigationId: String
}
