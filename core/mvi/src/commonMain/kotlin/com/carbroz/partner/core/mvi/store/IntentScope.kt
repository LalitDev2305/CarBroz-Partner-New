package com.carbroz.partner.core.mvi.store

/**
 * Execution scope exposed to an [IntentProcessor] for performing atomic state updates and emitting transient effects.
 *
 * @param State Immutable state representation.
 * @param Effect Volatile presentation event.
 */
interface IntentScope<State, Effect> {

    /**
     * Current snapshot of the Store state.
     */
    val currentState: State

    /**
     * Atomically transforms and updates the Store state.
     *
     * @param transform Pure transformation function taking current state and returning new state.
     */
    suspend fun updateState(transform: (State) -> State)

    /**
     * Emits a volatile presentation effect to active consumers.
     *
     * @param effect Transient event to emit.
     */
    suspend fun emitEffect(effect: Effect)
}

/**
 * Suspending functional processing contract executed for each incoming intent.
 */
typealias IntentProcessor<State, Intent, Effect> = suspend IntentScope<State, Effect>.(Intent) -> Unit
