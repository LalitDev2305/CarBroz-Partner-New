package com.carbroz.foundation.navigation

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class NavigationRestorationTest {
    private data class Destination(override val navigationId: String) : NavigationDestination

    @Test
    fun emptyPersistedStackFallsBackToRoot() {
        val root = Destination("root")

        val result = NavigationRestorationPolicy.restore(
            persisted = emptyList(),
            fallbackRoot = root,
            restorer = NavigationDestinationRestorer { null },
        )

        val fallback = assertIs<NavigationRestorationResult.Fallback>(result)
        assertEquals(NavigationRestorationFailure.EmptyStack, fallback.reason)
        assertEquals(listOf(root), fallback.state.backStack)
    }

    @Test
    fun unknownDestinationFallsBackToRoot() {
        val root = Destination("root")
        val persisted = listOf(RestoredDestination("known"), RestoredDestination("missing"))

        val result = NavigationRestorationPolicy.restore(
            persisted = persisted,
            fallbackRoot = root,
            restorer = NavigationDestinationRestorer { entry ->
                if (entry.navigationId == "known") Destination("known") else null
            },
        )

        val fallback = assertIs<NavigationRestorationResult.Fallback>(result)
        assertEquals(NavigationRestorationFailure.UnknownDestination, fallback.reason)
        assertEquals(listOf(root), fallback.state.backStack)
    }

    @Test
    fun validStackRestoresInOrder() {
        val root = Destination("root")
        val persisted = listOf(RestoredDestination("root"), RestoredDestination("details", payload = "42"))

        val result = NavigationRestorationPolicy.restore(
            persisted = persisted,
            fallbackRoot = root,
            restorer = NavigationDestinationRestorer { entry -> Destination(entry.navigationId) },
        )

        val restored = assertIs<NavigationRestorationResult.Restored>(result)
        assertEquals(listOf(Destination("root"), Destination("details")), restored.state.backStack)
    }
}
