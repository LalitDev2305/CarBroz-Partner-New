package com.carbroz.feature.splash

import com.carbroz.foundation.architecture.store.Store
import com.carbroz.foundation.lifecycle.AppLifecycleState
import com.carbroz.runtime.application.ApplicationRuntime
import com.carbroz.runtime.application.ApplicationRuntimeState
import com.carbroz.runtime.application.startup.StartupBlocker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

/** Presentation-only MVI owner for the static Splash feature. */
class SplashStore(
    private val runtime: ApplicationRuntime,
    parentScope: CoroutineScope,
) : Store<SplashIntent, SplashState> {
    private val parentJob = parentScope.coroutineContext[Job]
    private val scope = CoroutineScope(parentScope.coroutineContext + SupervisorJob(parentJob))
    private val mutableState = MutableStateFlow(runtime.state.value.toSplashState())
    private val effectChannel = Channel<SplashEffect>(capacity = Channel.BUFFERED)

    override val state: StateFlow<SplashState> = mutableState.asStateFlow()
    val effects: Flow<SplashEffect> = effectChannel.receiveAsFlow()

    private var lifecycleState: AppLifecycleState = AppLifecycleState.Unknown
    private var startupJob: Job? = null
    private var lastNavigatedAttempt: UInt? = null

    init {
        scope.launch {
            runtime.state.collectLatest { runtimeState ->
                mutableState.value = runtimeState.toSplashState()
                if (runtimeState is ApplicationRuntimeState.Ready && lastNavigatedAttempt != runtimeState.attempt) {
                    lastNavigatedAttempt = runtimeState.attempt
                    effectChannel.send(SplashEffect.Navigate(runtimeState.payload))
                }
            }
        }
    }

    override fun dispatch(intent: SplashIntent) {
        when (intent) {
            is SplashIntent.LifecycleChanged -> onLifecycleChanged(intent.state)
            SplashIntent.RetryClicked -> retryIfAllowed()
            SplashIntent.UpdateClicked -> openUpdateIfAvailable()
        }
    }

    fun close() {
        startupJob?.cancel()
        scope.cancel()
        effectChannel.close()
    }

    private fun onLifecycleChanged(state: AppLifecycleState) {
        lifecycleState = state
        when (state) {
            AppLifecycleState.Background -> {
                startupJob?.cancel()
                startupJob = null
            }

            AppLifecycleState.Foreground -> {
                if (runtime.state.value == ApplicationRuntimeState.Idle) startRuntime(retry = false)
            }

            AppLifecycleState.Unknown -> Unit
        }
    }

    private fun retryIfAllowed() {
        val allowed = when (val current = mutableState.value) {
            is SplashState.Maintenance -> current.retryEnabled
            is SplashState.Error -> current.retryEnabled
            else -> false
        }
        if (allowed) startRuntime(retry = true)
    }

    private fun openUpdateIfAvailable() {
        val update = mutableState.value as? SplashState.RequiredUpdate ?: return
        scope.launch { effectChannel.send(SplashEffect.OpenUpdateUri(update.updateUri)) }
    }

    private fun startRuntime(retry: Boolean) {
        if (lifecycleState != AppLifecycleState.Foreground) return
        if (startupJob?.isActive == true) return
        startupJob = scope.launch {
            if (retry) runtime.retry() else runtime.start()
        }
    }

    private fun ApplicationRuntimeState.toSplashState(): SplashState = when (this) {
        ApplicationRuntimeState.Idle,
        is ApplicationRuntimeState.Starting,
        -> SplashState.Loading

        is ApplicationRuntimeState.Ready -> SplashState.Ready

        is ApplicationRuntimeState.Blocked -> when (val reason = blocker) {
            is StartupBlocker.RequiredUpdate -> SplashState.RequiredUpdate(
                title = reason.title ?: "Update required",
                message = reason.message ?: "A newer version of CarBroz Partner is required to continue.",
                updateUri = reason.updateUri,
            )

            is StartupBlocker.Maintenance -> SplashState.Maintenance(
                title = reason.title ?: "We'll be back soon",
                message = reason.message ?: "CarBroz Partner is temporarily unavailable. Please try again shortly.",
                retryEnabled = reason.retryable,
            )
        }

        is ApplicationRuntimeState.Failed -> SplashState.Error(
            message = if (failure.recoverable) {
                "We couldn't connect to CarBroz right now. Check your connection and try again."
            } else {
                "CarBroz couldn't safely finish startup. Please restart the app or contact support."
            },
            retryEnabled = failure.recoverable,
        )
    }
}
