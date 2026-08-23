package com.carbroz.foundation.navigation

/**
 * Application-boundary coordinator for external navigation inputs.
 *
 * Deep links, prerequisite guards and restoration all converge on the same
 * [NavigationStore]. This prevents platform hosts, SDUI and persistence
 * adapters from mutating navigation state through independent paths.
 */
class NavigationCoordinator(
    private val store: NavigationStore,
) {
    fun handleDeepLink(
        request: DeepLinkRequest,
        resolver: DeepLinkResolver,
        guard: NavigationGuard,
    ): DeepLinkHandlingResult = when (val resolution = resolver.resolve(request)) {
        is DeepLinkResolution.Rejected -> DeepLinkHandlingResult.Rejected(resolution.reason)
        is DeepLinkResolution.Resolved -> DeepLinkHandlingResult.Accepted(
            store.dispatchGuarded(resolution.command, guard),
        )
    }

    fun restore(
        persisted: List<RestoredDestination>,
        fallbackRoot: NavigationDestination,
        restorer: NavigationDestinationRestorer,
    ): NavigationRestorationResult {
        val result = NavigationRestorationPolicy.restore(
            persisted = persisted,
            fallbackRoot = fallbackRoot,
            restorer = restorer,
        )
        store.restore(
            when (result) {
                is NavigationRestorationResult.Restored -> result.state
                is NavigationRestorationResult.Fallback -> result.state
            },
        )
        return result
    }
}

sealed interface DeepLinkHandlingResult {
    data class Accepted(val decision: GuardedNavigationDecision) : DeepLinkHandlingResult
    data class Rejected(val reason: DeepLinkRejectionReason) : DeepLinkHandlingResult
}
