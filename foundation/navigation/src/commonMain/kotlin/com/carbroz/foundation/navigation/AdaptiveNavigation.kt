package com.carbroz.foundation.navigation

/**
 * Presentation capacity available to navigation without coupling semantic
 * destinations to a particular adaptive/layout framework.
 */
enum class NavigationPresentationCapacity {
    SinglePane,
    ListDetail,
    SupportingPane,
}

/**
 * Semantic presentation preference supplied by a destination family.
 *
 * This affects only how destinations may be presented concurrently. It never
 * creates another back stack and never changes canonical navigation state.
 */
enum class NavigationPresentationPreference {
    Single,
    ListDetail,
    Supporting,
}

/** Resulting presentation mode for the current adaptive capacity. */
enum class NavigationPresentationMode {
    SinglePane,
    ListDetail,
    SupportingPane,
}

/** Pure policy used by the presentation boundary during resize/posture changes. */
object AdaptiveNavigationPolicy {
    fun resolve(
        capacity: NavigationPresentationCapacity,
        preference: NavigationPresentationPreference,
    ): NavigationPresentationMode = when (capacity) {
        NavigationPresentationCapacity.SinglePane -> NavigationPresentationMode.SinglePane
        NavigationPresentationCapacity.ListDetail -> when (preference) {
            NavigationPresentationPreference.Single -> NavigationPresentationMode.SinglePane
            NavigationPresentationPreference.ListDetail,
            NavigationPresentationPreference.Supporting,
            -> NavigationPresentationMode.ListDetail
        }
        NavigationPresentationCapacity.SupportingPane -> when (preference) {
            NavigationPresentationPreference.Single -> NavigationPresentationMode.SinglePane
            NavigationPresentationPreference.ListDetail -> NavigationPresentationMode.ListDetail
            NavigationPresentationPreference.Supporting -> NavigationPresentationMode.SupportingPane
        }
    }
}
