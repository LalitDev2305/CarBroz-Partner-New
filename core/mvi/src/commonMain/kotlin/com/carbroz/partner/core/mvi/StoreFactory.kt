package com.carbroz.partner.core.mvi

import com.carbroz.partner.core.mvi.internal.DefaultStore
import com.carbroz.partner.core.observability.logger.StructuredLogger
import kotlinx.coroutines.CoroutineScope

/**
 * Top-level factory function creating a thread-safe, lifecycle-bound [Store].
 *
 * @param scope Parent [CoroutineScope] whose cancellation disposes this store.
 * @param initialState Initial state value.
 * @param storeId Diagnostic identifier used for telemetry logging. Must not be blank.
 * @param logger Required [StructuredLogger] instance for observability.
 * @param processor Suspending lambda defining intent processing and state updates.
 */
public fun <State, Intent, Effect> createStore(
    scope: CoroutineScope,
    initialState: State,
    storeId: String,
    logger: StructuredLogger,
    processor: StoreProcessor<State, Intent, Effect>
): Store<State, Intent, Effect> {
    require(storeId.isNotBlank()) { "storeId must not be blank" }
    return DefaultStore(
        scope = scope,
        initialState = initialState,
        storeId = storeId,
        logger = logger,
        processor = processor
    )
}
