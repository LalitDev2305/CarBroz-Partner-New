package com.carbroz.foundation.architecture.store

import kotlinx.coroutines.flow.StateFlow

/**
 * Canonical state owner for an MVI/UDF presentation or runtime scope.
 *
 * A Store exposes immutable observable state and accepts intents. It does not
 * prescribe UI technology, navigation, persistence, or business rules.
 * Implementations must preserve unidirectional state flow and structured
 * concurrency for their owned lifecycle scope.
 */
interface Store<in Intent : Any, out State : Any> {
    val state: StateFlow<State>

    fun dispatch(intent: Intent)
}
