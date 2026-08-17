package com.carbroz.partner.core.mvi

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.CoroutineScope
import com.carbroz.partner.core.observability.logger.StructuredLogger

/**
 * Primary MVI Store contract exposing immutable state observation, intent dispatch, and transient effect emission.
 *
 * @param State Immutable state representation owned by [state].
 * @param Intent Semantic user or system event dispatched via [dispatch].
 * @param Effect Volatile presentation event emitted via [effects].
 */
interface Store<State, Intent, Effect> {

    /**
     * Read-only stream exposing current application or component state.
     */
    val state: StateFlow<State>

    /**
     * Volatile stream of presentation effects (e.g. snackbars, focus requests).
     *
     * Semantics: Transient, non-replayable, fan-out delivery. Exactly ONE active presentation collector is expected per Store.
     */
    val effects: Flow<Effect>

    /**
     * Suspending intent dispatch providing non-lossy coroutine backpressure.
     *
     * Semantics: Suspends when intent processing is in-flight under rendezvous backpressure.
     * Throws [kotlinx.coroutines.channels.ClosedSendChannelException] or the processor's fatal exception if the store is terminated.
     */
    suspend fun dispatch(intent: Intent)
}


/**
 * Top-level factory function creating a thread-safe, lifecycle-bound [Store].
 *
 * @param scope Parent [CoroutineScope] whose cancellation disposes this store.
 * @param initialState Initial state value.
 * @param storeId Diagnostic identifier used for telemetry logging.
 * @param logger Optional [StructuredLogger] instance for observability.
 * @param processor Suspending lambda defining intent processing and state updates.
 */
fun <State, Intent, Effect> createStore(
    scope: CoroutineScope,
    initialState: State,
    storeId: String,
    logger: StructuredLogger? = null,
    processor: IntentProcessor<State, Intent, Effect>
): Store<State, Intent, Effect> {
    return DefaultStore(
        scope = scope,
        initialState = initialState,
        storeId = storeId,
        logger = logger,
        processor = processor
    )
}
