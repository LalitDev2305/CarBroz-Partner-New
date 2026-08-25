package com.carbroz.feature.splash

import com.carbroz.foundation.lifecycle.AppLifecycleState
import com.carbroz.foundation.navigation.NavigationDestination
import com.carbroz.runtime.application.ApplicationRuntimeState

/** Static application entry destination. */
data object SplashDestination : NavigationDestination {
    override val navigationId: String = "splash"
}

sealed interface SplashIntent {
    data object Start : SplashIntent
    data object Retry : SplashIntent
    data class LifecycleChanged(val state: AppLifecycleState) : SplashIntent
}

data class SplashState(
    val runtimeState: ApplicationRuntimeState = ApplicationRuntimeState.Idle,
    val lifecycleState: AppLifecycleState = AppLifecycleState.Unknown,
) {
    val isLoading: Boolean
        get() = runtimeState is ApplicationRuntimeState.Idle ||
            runtimeState is ApplicationRuntimeState.Starting

    val failure: ApplicationRuntimeState.Failed?
        get() = runtimeState as? ApplicationRuntimeState.Failed

    val isReady: Boolean
        get() = runtimeState is ApplicationRuntimeState.Ready
}
