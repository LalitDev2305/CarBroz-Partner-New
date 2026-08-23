package com.carbroz.foundation.navigation

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class NavigationGuardPolicyTest {
    @Test
    fun allowedDestinationProceedsWithoutPendingState() {
        val destination = TestDestination("target")
        val command = NavigationCommand.Push(destination)

        val decision = NavigationGuardPolicy.apply(command) { NavigationGuardResult.Allowed }

        assertEquals(GuardedNavigationDecision.Proceed(command), decision)
    }

    @Test
    fun blockedDestinationRedirectsAndPreservesPendingIntent() {
        val destination = TestDestination("target")
        val recovery = TestDestination("sign-in")
        val prerequisite = TestPrerequisite("authenticated")

        val decision = NavigationGuardPolicy.apply(NavigationCommand.Push(destination)) {
            NavigationGuardResult.Blocked(prerequisite, recovery)
        }

        val redirect = assertIs<GuardedNavigationDecision.Redirect>(decision)
        assertEquals(NavigationCommand.Push(recovery), redirect.command)
        assertEquals(PendingDestination(destination, "authenticated"), redirect.pending)
    }

    @Test
    fun popCommandsBypassDestinationGuardEvaluation() {
        var evaluations = 0
        val decision = NavigationGuardPolicy.apply(NavigationCommand.Pop) {
            evaluations += 1
            NavigationGuardResult.Allowed
        }

        assertEquals(0, evaluations)
        assertEquals(GuardedNavigationDecision.Proceed(NavigationCommand.Pop), decision)
    }

    private data class TestDestination(
        override val navigationId: String,
    ) : NavigationDestination

    private data class TestPrerequisite(
        override val prerequisiteId: String,
    ) : NavigationPrerequisite
}
