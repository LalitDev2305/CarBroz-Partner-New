package com.carbroz.foundation.navigation

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class NavigationCoordinatorTest {
    @Test
    fun resolvedDeepLinkFlowsThroughGuardIntoCanonicalStore() {
        val root = TestDestination("root")
        val target = TestDestination("target")
        val signIn = TestDestination("sign-in")
        val store = NavigationStore(NavigationState(listOf(root)))
        val coordinator = NavigationCoordinator(store)

        val result = coordinator.handleDeepLink(
            request = DeepLinkRequest("carbroz://target"),
            resolver = DeepLinkResolver { DeepLinkResolution.Resolved(NavigationCommand.Push(target)) },
            guard = NavigationGuard {
                NavigationGuardResult.Blocked(TestPrerequisite("authenticated"), signIn)
            },
        )

        assertIs<DeepLinkHandlingResult.Accepted>(result)
        assertEquals(signIn, store.state.value.current)
        assertEquals(PendingDestination(target, "authenticated"), store.pending.value)
    }

    @Test
    fun rejectedDeepLinkDoesNotMutateCanonicalStore() {
        val root = TestDestination("root")
        val store = NavigationStore(NavigationState(listOf(root)))
        val coordinator = NavigationCoordinator(store)

        val result = coordinator.handleDeepLink(
            request = DeepLinkRequest("unsupported://target"),
            resolver = DeepLinkResolver { DeepLinkResolution.Rejected(DeepLinkRejectionReason.Unsupported) },
            guard = NavigationGuard { NavigationGuardResult.Allowed },
        )

        assertEquals(DeepLinkHandlingResult.Rejected(DeepLinkRejectionReason.Unsupported), result)
        assertEquals(listOf(root), store.state.value.backStack)
    }

    @Test
    fun restorationReplacesCanonicalStateAndClearsPendingDestination() {
        val root = TestDestination("root")
        val target = TestDestination("target")
        val signIn = TestDestination("sign-in")
        val store = NavigationStore(NavigationState(listOf(root)))
        val coordinator = NavigationCoordinator(store)
        store.dispatchGuarded(NavigationCommand.Push(target)) {
            NavigationGuardResult.Blocked(TestPrerequisite("authenticated"), signIn)
        }

        val result = coordinator.restore(
            persisted = listOf(RestoredDestination("target")),
            fallbackRoot = root,
            restorer = NavigationDestinationRestorer { if (it.navigationId == "target") target else null },
        )

        assertIs<NavigationRestorationResult.Restored>(result)
        assertEquals(listOf(target), store.state.value.backStack)
        assertEquals(null, store.pending.value)
    }

    private data class TestDestination(
        override val navigationId: String,
    ) : NavigationDestination

    private data class TestPrerequisite(
        override val prerequisiteId: String,
    ) : NavigationPrerequisite
}
