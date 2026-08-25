package com.carbroz.feature.splash

import com.carbroz.foundation.architecture.store.Store
import com.carbroz.foundation.lifecycle.AppLifecycleState
import com.carbroz.runtime.application.ApplicationRuntime
import com.carbroz.runtime.application.ApplicationRuntimeState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/**
 * Static splash state owner.
 *
 * The runtime remains the canonical owner of bootstrap state. This store only
 * adapts runtime + lifecycle state into presentation state and translates UI
 * intents into bootstrap commands.
 */
class SplashStore(
    private val runtime: ApplicationRuntime,
    parentScope: CoroutineScope,
) : Store<SplashIntent, SplashState> {
    private val scope = CoroutineScope(parentScope.coroutineContext + SupervisorJob())
    private val mutableState = MutableStateFlow(SplashState(runtimeState = runtime.state.value))
    override val state: StateFlow<SplashState> = mutableState.asStateFlow()

    private var bootstrapJob: Job? = null

    init {
        scope.launch {
            runtime.state.collectLatest { runtimeState ->
                mutableState.value = mutableState.value.copy(runtimeState = runtimeState)
            }
        }
    }

    override fun dispatch(intent: SplashIntent) {
        when (intent) {
            SplashIntent.Start -> startIfAllowed(retry = false)
            SplashIntent.Retry -> startIfAllowed(retry = true)
            is SplashIntent.LifecycleChanged -> onLifecycleChanged(intent.state)
        }
    }

    fun close() {
        bootstrapJob?.cancel()
        scope.cancel()
    }

    private fun onLifecycleChanged(state: AppLifecycleState) {
        mutableState.value = mutableState.value.copy(lifecycleState = state)
        if (state == AppLifecycleState.Background) {
            bootstrapJob?.cancel()
            bootstrapJob = null
        } else if (state == AppLifecycleState.Foreground && runtime.state.value == ApplicationRuntimeState.Idle) {
            startIfAllowed(retry = false)
        }
    }

    private fun startIfAllowed(retry: Boolean) {
        if (mutableState.value.lifecycleState == AppLifecycleState.Background) return
        if (bootstrapJob?.isActive == true) return

        bootstrapJob = scope.launch {
            if (retry) runtime.retry() else runtime.start()
        }
    }
}
