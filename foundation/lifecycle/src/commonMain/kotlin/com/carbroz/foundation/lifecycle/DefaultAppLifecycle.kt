package com.carbroz.foundation.lifecycle

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Thread-safe common lifecycle state holder fed by thin platform adapters.
 *
 * Duplicate transitions are harmless because [StateFlow] only exposes the latest
 * value. This class performs no platform detection and owns no coroutine scope.
 */
class DefaultAppLifecycle(
    initialState: AppLifecycleState = AppLifecycleState.Unknown,
) : AppLifecycleController {
    private val mutableState = MutableStateFlow(initialState)

    override val state: StateFlow<AppLifecycleState> = mutableState.asStateFlow()

    override fun moveTo(state: AppLifecycleState) {
        mutableState.value = state
    }
}
