package com.carbroz.foundation.architecture.reducer

/**
 * Pure state transition contract for MVI/UDF state machines.
 *
 * Implementations must be deterministic and side-effect free so transitions
 * remain independently testable and reproducible.
 */
fun interface Reducer<State : Any, in Result : Any> {
    fun reduce(previous: State, result: Result): State
}
