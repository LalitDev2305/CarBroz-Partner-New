package com.carbroz.foundation.navigation

/**
 * Product-neutral prerequisite evaluated before a destination is applied.
 * Concrete prerequisite types live with the subsystem that owns their meaning.
 */
interface NavigationPrerequisite {
    val prerequisiteId: String
}

/** Result of evaluating whether a destination may be entered now. */
sealed interface NavigationGuardResult {
    data object Allowed : NavigationGuardResult
    data class Blocked(
        val prerequisite: NavigationPrerequisite,
        val recoveryDestination: NavigationDestination,
    ) : NavigationGuardResult
}

/**
 * Evaluates destination prerequisites without depending on UI or Navigation 3.
 */
fun interface NavigationGuard {
    fun evaluate(destination: NavigationDestination): NavigationGuardResult
}

/**
 * Semantic destination deferred until a prerequisite has been satisfied.
 *
 * This is application-owned navigation state rather than a second framework
 * stack. Only one pending destination is retained because each guard recovery
 * flow represents one interrupted navigation intent at a time.
 */
data class PendingDestination(
    val destination: NavigationDestination,
    val prerequisiteId: String,
)

/** Result of applying guard policy to a navigation request. */
sealed interface GuardedNavigationDecision {
    data class Proceed(val command: NavigationCommand) : GuardedNavigationDecision
    data class Redirect(
        val command: NavigationCommand,
        val pending: PendingDestination,
    ) : GuardedNavigationDecision
}

/**
 * Converts guarded destination entry into normal semantic navigation commands.
 */
object NavigationGuardPolicy {
    fun apply(
        command: NavigationCommand,
        guard: NavigationGuard,
    ): GuardedNavigationDecision =
        when (command) {
            is NavigationCommand.Push -> evaluate(command.destination, command, guard)
            is NavigationCommand.ReplaceTop -> evaluate(command.destination, command, guard)
            is NavigationCommand.ResetTo -> evaluate(command.destination, command, guard)
            NavigationCommand.Pop,
            is NavigationCommand.PopTo,
            -> GuardedNavigationDecision.Proceed(command)
        }

    private fun evaluate(
        destination: NavigationDestination,
        original: NavigationCommand,
        guard: NavigationGuard,
    ): GuardedNavigationDecision =
        when (val result = guard.evaluate(destination)) {
            NavigationGuardResult.Allowed -> GuardedNavigationDecision.Proceed(original)
            is NavigationGuardResult.Blocked -> GuardedNavigationDecision.Redirect(
                command = NavigationCommand.Push(result.recoveryDestination),
                pending = PendingDestination(
                    destination = destination,
                    prerequisiteId = result.prerequisite.prerequisiteId,
                ),
            )
        }
}
