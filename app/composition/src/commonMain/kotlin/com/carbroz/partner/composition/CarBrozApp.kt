package com.carbroz.partner.composition

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.withFrameNanos
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.carbroz.data.network.NetworkDataSource
import com.carbroz.feature.splash.ReferenceDestination
import com.carbroz.feature.splash.SplashDestination
import com.carbroz.feature.splash.SplashIntent
import com.carbroz.feature.splash.SplashScreen
import com.carbroz.feature.splash.SplashStore
import com.carbroz.foundation.adaptive.AdaptiveLayoutProvider
import com.carbroz.foundation.configuration.AppConfiguration
import com.carbroz.foundation.designsystem.CarBrozTheme
import com.carbroz.foundation.lifecycle.AppLifecycle
import com.carbroz.foundation.navigation.Navigation3Host
import com.carbroz.foundation.navigation.NavigationCommand
import com.carbroz.foundation.navigation.NavigationDestinationContent
import com.carbroz.foundation.navigation.NavigationStore
import com.carbroz.foundation.observability.Observability
import com.carbroz.foundation.observability.PerformanceMetric
import com.carbroz.foundation.time.Clock
import com.carbroz.runtime.application.ApplicationRuntime
import org.koin.compose.koinInject

/** Application composition root. */
@Composable
fun CarBrozApp() {
    val navigationStore = koinInject<NavigationStore>()
    val syncActivationCoordinator = koinInject<SyncActivationCoordinator>()
    val observability = koinInject<Observability>()
    val clock = koinInject<Clock>()
    val runtime = koinInject<ApplicationRuntime>()
    val lifecycle = koinInject<AppLifecycle>()
    val bootstrapRoutes = koinInject<BootstrapRouteStore>()
    val configuration = koinInject<AppConfiguration>()
    val network = koinInject<NetworkDataSource>()
    val networkActions = koinInject<NetworkActionExecutor>()
    val capabilityActions = koinInject<CapabilityActionExecutor>()

    val navigationState by navigationStore.state.collectAsStateWithLifecycle()
    val lifecycleState by lifecycle.state.collectAsStateWithLifecycle()
    val applicationScope = rememberCoroutineScope()

    val splashStore = remember(runtime, applicationScope) { SplashStore(runtime, applicationScope) }
    val splashState by splashStore.state.collectAsStateWithLifecycle()

    val referenceRuntime = remember(configuration) { createReferenceSduiRuntime(configuration) }
    val referenceStore = remember(
        network,
        referenceRuntime,
        networkActions,
        capabilityActions,
        applicationScope,
    ) {
        ReferenceSduiStore(
            network = network,
            pipeline = referenceRuntime.pipeline,
            actionPreparer = referenceRuntime.actions,
            networkActions = networkActions,
            capabilityActions = capabilityActions,
            parentScope = applicationScope,
        )
    }
    val referenceState by referenceStore.state.collectAsStateWithLifecycle()
    val firstRenderStartedAt = remember { clock.nowEpochMilliseconds() }

    val destinationContent = NavigationDestinationContent { destination ->
        when (destination) {
            SplashDestination -> SplashScreen(
                state = splashState,
                onRetry = { splashStore.dispatch(SplashIntent.Retry) },
            )

            ReferenceDestination -> ReferenceSduiScreen(
                state = referenceState,
                dispatcher = referenceRuntime.renderer,
                onRetry = referenceStore::load,
                onCommand = referenceStore::execute,
                onRenderFailure = referenceStore::reportRenderFailure,
            )

            else -> error("No composition content registered for ${destination.navigationId}.")
        }
    }

    LaunchedEffect(Unit) {
        withFrameNanos { }
        observability.performance(
            PerformanceMetric(
                name = "app.first_render",
                durationMillis = (clock.nowEpochMilliseconds() - firstRenderStartedAt).coerceAtLeast(0L),
            ),
        )
    }

    LaunchedEffect(lifecycleState) {
        splashStore.dispatch(SplashIntent.LifecycleChanged(lifecycleState))
    }

    LaunchedEffect(Unit) {
        splashStore.dispatch(SplashIntent.Start)
    }

    LaunchedEffect(splashState.isReady) {
        if (splashState.isReady && navigationStore.state.value.current == SplashDestination) {
            val resolvedRoute = bootstrapRoutes.current()
                ?: error("Application runtime became ready without a resolved bootstrap route.")
            navigationStore.dispatch(
                NavigationCommand.ResetTo(resolvedRoute.toNavigationDestination()),
            )
        }
    }

    LaunchedEffect(navigationState.current) {
        if (navigationState.current == ReferenceDestination && referenceStore.state.value.screen == null) {
            referenceStore.load()
        }
    }

    DisposableEffect(syncActivationCoordinator, applicationScope) {
        val activationJob = syncActivationCoordinator.start(applicationScope)
        onDispose { activationJob.cancel() }
    }

    DisposableEffect(splashStore) {
        onDispose { splashStore.close() }
    }

    DisposableEffect(referenceStore) {
        onDispose { referenceStore.close() }
    }

    CarBrozTheme {
        AdaptiveLayoutProvider {
            Navigation3Host(
                state = navigationState,
                destinationContent = destinationContent,
                onCommand = navigationStore::dispatch,
            )
        }
    }
}
