package com.carbroz.foundation.navigation

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Canonical application-owned navigation state holder.
 *
 * This store owns the semantic back stack and pending guarded destination.
 * Navigation 3 only presents [state]; it never owns an independent CarBroz
 * stack. The store is framework-independent and can survive UI recomposition
 * and adaptive resize as long as its owning application scope survives.
 */
class NavigationStore(
    initialState: NavigationState,
) {
    private val mutableState = MutableStateFlow(initialState)
    val state: StateFlow<NavigationState> = mutableState.asStateFlow()

    private val mutablePending = MutableStateFlow<PendingDestination?>(null)
    val pending: StateFlow<PendingDestination?> = mutablePending.asStateFlow()

    fun dispatch(command: NavigationCommand): NavigationTransition {
        val transition = NavigationReducer.reduce(mutableState.value, command)
        if (transition is NavigationTransition.Applied) {
            mutableState.value = transition.state
        }
        return transition
    }

    fun dispatchGuarded(
        command: NavigationCommand,
        guard: NavigationGuard,
    ): GuardedNavigationDecision {
        val decision = NavigationGuardPolicy.apply(command, guard)
        when (decision) {
            is GuardedNavigationDecision.Proceed -> dispatch(decision.command)
            is GuardedNavigationDecision.Redirect -> {
                mutablePending.value = decision.pending
                dispatch(decision.command)
            }
        }
        return decision
    }

    fun resumePending(prerequisiteId: String): NavigationTransition? {
        val currentPending = mutablePending.value ?: return null
        if (currentPending.prerequisiteId != prerequisiteId) return null

        mutablePending.value = null
        return dispatch(NavigationCommand.Push(currentPending.destination))
    }

    fun clearPending() {
        mutablePending.value = null
    }

    fun restore(state: NavigationState) {
        mutableState.value = state
        mutablePending.value = null
    }
}
