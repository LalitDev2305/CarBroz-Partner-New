package com.carbroz.feature.splash

import com.carbroz.foundation.architecture.store.Store
import com.carbroz.foundation.lifecycle.AppLifecycleState
import com.carbroz.runtime.application.startup.ResolveStartupUseCase
import com.carbroz.runtime.application.startup.StartupResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

/** Single MVI/UDF state owner for the static Splash startup presentation. */
class SplashStore(
    private val resolveStartup: ResolveStartupUseCase,
    parentScope: CoroutineScope,
) : Store<SplashIntent, SplashState> {
    private val parentJob = parentScope.coroutineContext[Job]
    private val scope = CoroutineScope(parentScope.coroutineContext + SupervisorJob(parentJob))
    private val mutableState = MutableStateFlow<SplashState>(SplashState.Loading)
    private val effectChannel = Channel<SplashEffect>(capacity = Channel.BUFFERED)

    override val state: StateFlow<SplashState> = mutableState.asStateFlow()
    val effects: Flow<SplashEffect> = effectChannel.receiveAsFlow()

    private var lifecycleState: AppLifecycleState = AppLifecycleState.Unknown
    private var startupJob: Job? = null
    private var startupStarted = false

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
                if (startupJob?.isActive == true) {
                    startupJob?.cancel()
                    startupJob = null
                    startupStarted = false
                }
            }

            AppLifecycleState.Foreground -> if (!startupStarted) startStartup()
            AppLifecycleState.Unknown -> Unit
        }
    }

    private fun retryIfAllowed() {
        val allowed = when (val current = mutableState.value) {
            is SplashState.Maintenance -> current.retryEnabled
            is SplashState.Error -> current.retryEnabled
            else -> false
        }
        if (allowed) {
            startupStarted = false
            startStartup()
        }
    }

    private fun openUpdateIfAvailable() {
        val update = mutableState.value as? SplashState.RequiredUpdate ?: return
        scope.launch { effectChannel.send(SplashEffect.OpenUpdateUri(update.updateUri)) }
    }

    private fun startStartup() {
        if (lifecycleState != AppLifecycleState.Foreground) return
        if (startupJob?.isActive == true) return
        startupStarted = true
        mutableState.value = SplashState.Loading
        startupJob = scope.launch {
            when (val result = resolveStartup()) {
                is StartupResult.Ready -> {
                    mutableState.value = SplashState.Ready
                    effectChannel.send(SplashEffect.Navigate(result.destination))
                }

                is StartupResult.RequiredUpdate -> {
                    mutableState.value = SplashState.RequiredUpdate(
                        title = result.title,
                        message = result.message,
                        updateUri = result.updateUri,
                    )
                }

                is StartupResult.Maintenance -> {
                    mutableState.value = SplashState.Maintenance(
                        title = result.title ?: "We'll be back soon",
                        message = result.message ?: "CarBroz Partner is temporarily unavailable. Please try again shortly.",
                        retryEnabled = result.retryable,
                    )
                }

                is StartupResult.Failure -> {
                    mutableState.value = SplashState.Error(
                        message = if (result.recoverable) {
                            "We couldn't connect to CarBroz right now. Check your connection and try again."
                        } else {
                            "CarBroz couldn't safely finish startup. Please restart the app or contact support."
                        },
                        retryEnabled = result.recoverable,
                    )
                }
            }
        }
    }
}
