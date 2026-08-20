package com.carbroz.partner.feature.splash.store

import com.carbroz.partner.core.mvi.Store
import com.carbroz.partner.core.mvi.createStore
import com.carbroz.partner.core.observability.logger.StructuredLogger
import com.carbroz.partner.feature.splash.orchestrator.StartupDestination
import com.carbroz.partner.feature.splash.orchestrator.StartupOrchestrator
import com.carbroz.partner.feature.splash.orchestrator.StartupResult
import kotlinx.coroutines.CoroutineScope

/**
 * Immutable state of the Native Splash Screen.
 */
public sealed interface SplashState {
    public data object Initial : SplashState
    public data object Loading : SplashState
    public data class Success(val destination: StartupDestination) : SplashState
    public data class Error(val message: String) : SplashState
}

/**
 * User or system intents targeted at the Splash Store.
 */
public sealed interface SplashIntent {
    public data object Initialize : SplashIntent
    public data object Retry : SplashIntent
}

/**
 * One-time side effects emitted by the Splash Store.
 */
public sealed interface SplashEffect {
    public data class NavigateToDestination(val destination: StartupDestination) : SplashEffect
}

/**
 * MVI Store managing Splash state transitions and startup orchestration execution.
 */
public class SplashStore(
    scope: CoroutineScope,
    logger: StructuredLogger,
    private val orchestrator: StartupOrchestrator,
    private val delegateStore: Store<SplashState, SplashIntent, SplashEffect> = createStore(
        scope = scope,
        initialState = SplashState.Initial,
        storeId = "SplashStore",
        logger = logger,
        processor = { intent ->
            when (intent) {
                is SplashIntent.Initialize, is SplashIntent.Retry -> {
                    updateState { SplashState.Loading }
                    when (val result = orchestrator.initialize()) {
                        is StartupResult.Ready -> {
                            updateState { SplashState.Success(result.destination) }
                            emitEffect(SplashEffect.NavigateToDestination(result.destination))
                        }
                        is StartupResult.Failure -> {
                            updateState { SplashState.Error(result.message) }
                        }
                    }
                }
            }
        }
    )
) : Store<SplashState, SplashIntent, SplashEffect> by delegateStore
