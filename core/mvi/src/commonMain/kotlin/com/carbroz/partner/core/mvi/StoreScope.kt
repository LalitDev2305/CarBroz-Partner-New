package com.carbroz.partner.core.mvi

/**
 * Execution scope exposed to a [StoreProcessor] for performing atomic state updates and emitting transient effects.
 *
 * @param State Immutable state representation.
 * @param Effect Volatile presentation event.
 */
public interface StoreScope<State, Effect> {

    /**
     * Current snapshot of the Store state.
     */
    public val currentState: State

    /**
     * Synchronously and atomically transforms and updates the Store state.
     *
     * @param transform Pure transformation function taking current state and returning new state.
     */
    public fun updateState(transform: (State) -> State)

    /**
     * Emits a volatile presentation effect to active consumers.
     *
     * @param effect Transient event to emit.
     */
    public suspend fun emitEffect(effect: Effect)
}

/**
 * Suspending functional processing contract executed for each incoming intent.
 */
public typealias StoreProcessor<State, Intent, Effect> = suspend StoreScope<State, Effect>.(Intent) -> Unit
