package com.carbroz.feature.splash

import com.carbroz.foundation.lifecycle.AppLifecycleState
import com.carbroz.foundation.navigation.NavigationDestination
import com.carbroz.runtime.application.startup.StartupDestination

/** Static application entry destination. */
data object SplashDestination : NavigationDestination {
    override val navigationId: String = "splash"
}

sealed interface SplashIntent {
    data class LifecycleChanged(val state: AppLifecycleState) : SplashIntent
    data object RetryClicked : SplashIntent
    data object UpdateClicked : SplashIntent
}

sealed interface SplashState {
    data object Loading : SplashState

    data class RequiredUpdate(
        val title: String,
        val message: String,
        val updateUri: String,
    ) : SplashState

    data class Maintenance(
        val title: String,
        val message: String,
        val retryEnabled: Boolean,
    ) : SplashState

    data class Error(
        val message: String,
        val retryEnabled: Boolean,
    ) : SplashState

    data object Ready : SplashState
}

sealed interface SplashEffect {
    data class Navigate(val destination: StartupDestination) : SplashEffect
    data class OpenUpdateUri(val uri: String) : SplashEffect
}
