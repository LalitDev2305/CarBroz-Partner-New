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
     * Semantics:
     * - Transient presentation effects intended for UI presentation.
     * - Exactly one logical presentation collector is supported per Store instance.
     * - Retained effects are delivered in FIFO order.
     * - Consumed effects are not replayed to late collectors.
     * - Queue is explicitly bounded; if full, the newest effect is dropped (DROP_NEWEST).
     * - Overflow does not block intent processing or state updates.
     * - Does not support broadcast or fan-out semantics.
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
