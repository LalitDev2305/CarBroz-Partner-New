package com.carbroz.foundation.architecture.state

import com.carbroz.foundation.architecture.reducer.Reducer

/**
 * Named state-transition boundary for deterministic architecture logic.
 *
 * This type deliberately wraps only a [Reducer]. It gives workflow/runtime code
 * an explicit state-machine vocabulary without creating a second state owner or
 * introducing framework lifecycle concerns into the architecture kernel.
 */
class StateMachine<State : Any, Result : Any>(
    private val reducer: Reducer<State, Result>,
) {
    fun transition(state: State, result: Result): State = reducer.reduce(state, result)
}
