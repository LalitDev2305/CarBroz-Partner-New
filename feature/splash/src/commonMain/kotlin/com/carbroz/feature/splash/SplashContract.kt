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

enum class SplashPhase {
    INITIALIZING,
    RESTORING_SESSION,
    FETCHING_CONFIGURATION,
    PREPARING_DESTINATION,
    READY,
    FAILED,
    FORCE_UPDATE,
    MAINTENANCE,
}

data class SplashState(
    val runtimeState: ApplicationRuntimeState = ApplicationRuntimeState.Idle,
    val bootstrapState: BootstrapState = BootstrapState.Idle,
    val lifecycleState: AppLifecycleState = AppLifecycleState.Unknown,
) {
    val phase: SplashPhase
        get() = when {
            bootstrapState is BootstrapState.ForceUpdate -> SplashPhase.FORCE_UPDATE
            bootstrapState is BootstrapState.Maintenance -> SplashPhase.MAINTENANCE
            runtimeState is ApplicationRuntimeState.Failed -> SplashPhase.FAILED
            runtimeState is ApplicationRuntimeState.Ready && bootstrapState is BootstrapState.Ready -> SplashPhase.READY
            bootstrapState is BootstrapState.Fetching -> SplashPhase.FETCHING_CONFIGURATION
            runtimeState is ApplicationRuntimeState.Starting -> SplashPhase.RESTORING_SESSION
            runtimeState is ApplicationRuntimeState.Ready -> SplashPhase.PREPARING_DESTINATION
            else -> SplashPhase.INITIALIZING
        }

    val isLoading: Boolean
        get() = phase in setOf(
            SplashPhase.INITIALIZING,
            SplashPhase.RESTORING_SESSION,
            SplashPhase.FETCHING_CONFIGURATION,
            SplashPhase.PREPARING_DESTINATION,
        )

    val failure: ApplicationRuntimeState.Failed?
        get() = runtimeState as? ApplicationRuntimeState.Failed

    val forceUpdate: BootstrapState.ForceUpdate?
        get() = bootstrapState as? BootstrapState.ForceUpdate

    val maintenance: BootstrapState.Maintenance?
        get() = bootstrapState as? BootstrapState.Maintenance

    val isReady: Boolean
        get() = phase == SplashPhase.READY

    val statusTitle: String
        get() = when (phase) {
            SplashPhase.INITIALIZING -> "Loading..."
            SplashPhase.RESTORING_SESSION -> "Preparing your account..."
            SplashPhase.FETCHING_CONFIGURATION -> "Loading your partner experience..."
            SplashPhase.PREPARING_DESTINATION -> "Almost ready..."
            SplashPhase.READY -> "Ready"
            SplashPhase.FAILED -> "Unable to start"
            SplashPhase.FORCE_UPDATE -> forceUpdate?.policy?.title ?: "Update required"
            SplashPhase.MAINTENANCE -> maintenance?.policy?.title ?: "We'll be back soon"
        }

    val statusMessage: String
        get() = when (phase) {
            SplashPhase.INITIALIZING,
            SplashPhase.RESTORING_SESSION,
            SplashPhase.FETCHING_CONFIGURATION,
            SplashPhase.PREPARING_DESTINATION,
            SplashPhase.READY,
            -> "Preparing your partner experience"
            SplashPhase.FAILED -> failure?.failure?.toString() ?: "Startup failed. Please try again."
            SplashPhase.FORCE_UPDATE -> forceUpdate?.policy?.message
                ?: "A newer version of CarBroz Partner is required to continue."
            SplashPhase.MAINTENANCE -> maintenance?.policy?.message
                ?: "CarBroz Partner is temporarily unavailable. Please try again shortly."
        }
}
