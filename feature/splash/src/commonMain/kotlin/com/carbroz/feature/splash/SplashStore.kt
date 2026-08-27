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
 * Static splash presentation owner.
 *
 * [ApplicationRuntime] remains the owner of startup execution and [BootstrapStore] remains the owner of
 * the validated bootstrap outcome. This store only combines those states for native Splash presentation
 * and translates UI intents into startup commands.
 */
class SplashStore(
    private val runtime: ApplicationRuntime,
    private val bootstrap: BootstrapStore,
    parentScope: CoroutineScope,
) : Store<SplashIntent, SplashState> {
    private val parentJob = parentScope.coroutineContext[Job]
    private val scope = CoroutineScope(parentScope.coroutineContext + SupervisorJob(parentJob))
    private val mutableState = MutableStateFlow(
        SplashState(
            runtimeState = runtime.state.value,
            bootstrapState = bootstrap.state.value,
        ),
    )
    override val state: StateFlow<SplashState> = mutableState.asStateFlow()

    private var startupJob: Job? = null

    init {
        scope.launch {
            runtime.state.collectLatest { runtimeState ->
                mutableState.value = mutableState.value.copy(runtimeState = runtimeState)
            }
        }
        scope.launch {
            bootstrap.state.collectLatest { bootstrapState ->
                mutableState.value = mutableState.value.copy(bootstrapState = bootstrapState)
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
        startupJob?.cancel()
        scope.cancel()
    }

    private fun onLifecycleChanged(state: AppLifecycleState) {
        mutableState.value = mutableState.value.copy(lifecycleState = state)
        if (state == AppLifecycleState.Background) {
            startupJob?.cancel()
            startupJob = null
        } else if (state == AppLifecycleState.Foreground && runtime.state.value == ApplicationRuntimeState.Idle) {
            startIfAllowed(retry = false)
        }
    }

    private fun startIfAllowed(retry: Boolean) {
        if (mutableState.value.lifecycleState == AppLifecycleState.Background) return
        if (startupJob?.isActive == true) return

        startupJob = scope.launch {
            if (retry) {
                bootstrap.reset()
                runtime.retry()
            } else {
                runtime.start()
            }
        }
    }
}
