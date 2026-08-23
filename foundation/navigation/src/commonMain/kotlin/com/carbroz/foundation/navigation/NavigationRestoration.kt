package com.carbroz.foundation.navigation

/**
 * Stable serialized semantic destination supplied by the subsystem that owns
 * the concrete destination type.
 */
data class RestoredDestination(
    val navigationId: String,
    val payload: String? = null,
)

/**
 * Recreates semantic destinations from persisted restoration data. Concrete
 * codecs/registries may be introduced with real destination types; this
 * contract keeps restoration independent of Navigation 3 framework classes.
 */
fun interface NavigationDestinationRestorer {
    fun restore(destination: RestoredDestination): NavigationDestination?
}

/** Outcome of attempting to restore an application-owned back stack. */
sealed interface NavigationRestorationResult {
    data class Restored(val state: NavigationState) : NavigationRestorationResult
    data class Fallback(val state: NavigationState, val reason: NavigationRestorationFailure) : NavigationRestorationResult
}

enum class NavigationRestorationFailure {
    EmptyStack,
    UnknownDestination,
}

/** Pure restoration policy with safe fallback to a known root destination. */
object NavigationRestorationPolicy {
    fun restore(
        persisted: List<RestoredDestination>,
        fallbackRoot: NavigationDestination,
        restorer: NavigationDestinationRestorer,
    ): NavigationRestorationResult {
        if (persisted.isEmpty()) {
            return NavigationRestorationResult.Fallback(
                state = NavigationState(listOf(fallbackRoot)),
                reason = NavigationRestorationFailure.EmptyStack,
            )
        }

        val restored = ArrayList<NavigationDestination>(persisted.size)
        for (entry in persisted) {
            val destination = restorer.restore(entry)
                ?: return NavigationRestorationResult.Fallback(
                    state = NavigationState(listOf(fallbackRoot)),
                    reason = NavigationRestorationFailure.UnknownDestination,
                )
            restored += destination
        }

        return NavigationRestorationResult.Restored(NavigationState(restored))
    }
}
