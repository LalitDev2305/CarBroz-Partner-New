package com.carbroz.partner.core.mvi

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

/**
 * Primary MVI Store contract exposing immutable state observation, intent dispatch, and transient effect emission.
 *
 * @param State Immutable state representation owned by [state].
 * @param Intent Semantic user or system event dispatched via [dispatch].
 * @param Effect Volatile presentation event emitted via [effects].
 */
public interface Store<State, Intent, Effect> {

    /**
     * Read-only stream exposing current application or component state.
     */
    public val state: StateFlow<State>

    /**
     * Volatile stream of presentation effects (e.g. snackbars, focus requests).
     *
     * Semantics: Queued, single-consumption, FIFO delivery. Exactly ONE active presentation collector is supported per Store.
     */
    public val effects: Flow<Effect>

    /**
     * Suspending intent dispatch providing non-lossy coroutine backpressure.
     *
     * Semantics: Suspends when intent processing is in-flight under rendezvous backpressure.
     * Throws [kotlinx.coroutines.channels.ClosedSendChannelException] or the processor's fatal exception if the store is terminated.
     */
    public suspend fun dispatch(intent: Intent)
}
