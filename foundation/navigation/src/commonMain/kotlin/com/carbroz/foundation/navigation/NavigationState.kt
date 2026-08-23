package com.carbroz.foundation.navigation

/**
 * Immutable semantic view of the application back stack.
 *
 * The list is never empty: the root destination remains present unless the
 * application explicitly resets navigation to a new root.
 */
data class NavigationState(
    val backStack: List<NavigationDestination>,
) {
    init {
        require(backStack.isNotEmpty()) { "Navigation back stack must contain a root destination." }
    }

    val current: NavigationDestination
        get() = backStack.last()
}

sealed interface NavigationCommand {
    data class Push(val destination: NavigationDestination) : NavigationCommand
    data class ReplaceTop(val destination: NavigationDestination) : NavigationCommand
    data class ResetTo(val destination: NavigationDestination) : NavigationCommand
    data object Pop : NavigationCommand
    data class PopTo(
        val navigationId: String,
        val inclusive: Boolean = false,
    ) : NavigationCommand
}

sealed interface NavigationTransition {
    data class Applied(val state: NavigationState) : NavigationTransition
    data class Ignored(val state: NavigationState, val reason: NavigationIgnoreReason) : NavigationTransition
}

enum class NavigationIgnoreReason {
    AlreadyAtRoot,
    DestinationNotFound,
}

/**
 * Pure deterministic navigation transition policy.
 *
 * Guarding/authentication decisions happen before this reducer. This type owns
 * stack semantics only and contains no Compose or Navigation 3 framework code.
 */
object NavigationReducer {
    fun reduce(state: NavigationState, command: NavigationCommand): NavigationTransition =
        when (command) {
            is NavigationCommand.Push -> applied(state.backStack + command.destination)
            is NavigationCommand.ReplaceTop -> applied(state.backStack.dropLast(1) + command.destination)
            is NavigationCommand.ResetTo -> applied(listOf(command.destination))
            NavigationCommand.Pop -> {
                if (state.backStack.size == 1) {
                    NavigationTransition.Ignored(state, NavigationIgnoreReason.AlreadyAtRoot)
                } else {
                    applied(state.backStack.dropLast(1))
                }
            }
            is NavigationCommand.PopTo -> popTo(state, command)
        }

    private fun popTo(
        state: NavigationState,
        command: NavigationCommand.PopTo,
    ): NavigationTransition {
        val index = state.backStack.indexOfLast { it.navigationId == command.navigationId }
        if (index < 0) {
            return NavigationTransition.Ignored(state, NavigationIgnoreReason.DestinationNotFound)
        }

        val retainedCount = if (command.inclusive) index else index + 1
        if (retainedCount <= 0) {
            return NavigationTransition.Ignored(state, NavigationIgnoreReason.AlreadyAtRoot)
        }

        val next = state.backStack.take(retainedCount)
        return if (next == state.backStack) {
            NavigationTransition.Applied(state)
        } else {
            applied(next)
        }
    }

    private fun applied(backStack: List<NavigationDestination>): NavigationTransition.Applied =
        NavigationTransition.Applied(NavigationState(backStack))
}
