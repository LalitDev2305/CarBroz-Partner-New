package com.carbroz.foundation.navigation

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertSame

class NavigationStoreTest {
    private data class Destination(override val navigationId: String) : NavigationDestination
    private data class Prerequisite(override val prerequisiteId: String) : NavigationPrerequisite

    @Test
    fun dispatchUpdatesCanonicalStateOnAppliedTransition() {
        val root = Destination("root")
        val next = Destination("next")
        val store = NavigationStore(NavigationState(listOf(root)))

        val transition = store.dispatch(NavigationCommand.Push(next))

        assertEquals(listOf(root, next), store.state.value.backStack)
        assertEquals(listOf(root, next), (transition as NavigationTransition.Applied).state.backStack)
    }

    @Test
    fun ignoredTransitionDoesNotReplaceStateInstance() {
        val root = Destination("root")
        val initial = NavigationState(listOf(root))
        val store = NavigationStore(initial)

        store.dispatch(NavigationCommand.Pop)

        assertSame(initial, store.state.value)
    }

    @Test
    fun blockedNavigationStoresPendingAndRedirects() {
        val root = Destination("root")
        val target = Destination("target")
        val recovery = Destination("recovery")
        val prerequisite = Prerequisite("auth")
        val store = NavigationStore(NavigationState(listOf(root)))
        val guard = NavigationGuard {
            NavigationGuardResult.Blocked(prerequisite, recovery)
        }

        store.dispatchGuarded(NavigationCommand.Push(target), guard)

        assertEquals(listOf(root, recovery), store.state.value.backStack)
        assertEquals(PendingDestination(target, "auth"), store.pending.value)
    }

    @Test
    fun resumePendingOnlyMatchesRequiredPrerequisite() {
        val root = Destination("root")
        val target = Destination("target")
        val recovery = Destination("recovery")
        val prerequisite = Prerequisite("auth")
        val store = NavigationStore(NavigationState(listOf(root)))
        val guard = NavigationGuard {
            NavigationGuardResult.Blocked(prerequisite, recovery)
        }
        store.dispatchGuarded(NavigationCommand.Push(target), guard)

        assertNull(store.resumePending("other"))
        assertEquals(PendingDestination(target, "auth"), store.pending.value)

        store.resumePending("auth")

        assertNull(store.pending.value)
        assertEquals(listOf(root, recovery, target), store.state.value.backStack)
    }

    @Test
    fun restoreReplacesStateAndClearsPendingIntent() {
        val root = Destination("root")
        val target = Destination("target")
        val recovery = Destination("recovery")
        val store = NavigationStore(NavigationState(listOf(root)))
        val guard = NavigationGuard {
            NavigationGuardResult.Blocked(Prerequisite("auth"), recovery)
        }
        store.dispatchGuarded(NavigationCommand.Push(target), guard)

        val restored = NavigationState(listOf(root, target))
        store.restore(restored)

        assertSame(restored, store.state.value)
        assertNull(store.pending.value)
    }
}
