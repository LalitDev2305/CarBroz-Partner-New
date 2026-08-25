package com.carbroz.partner.composition

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.carbroz.feature.splash.ReferenceDestination
import com.carbroz.feature.splash.SplashDestination
import com.carbroz.feature.splash.SplashIntent
import com.carbroz.feature.splash.SplashScreen
import com.carbroz.feature.splash.SplashStore
import com.carbroz.foundation.adaptive.AdaptiveLayoutProvider
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
    val navigationState by navigationStore.state.collectAsStateWithLifecycle()
    val lifecycleState by lifecycle.state.collectAsStateWithLifecycle()
    val applicationScope = rememberCoroutineScope()
    val splashStore = remember(runtime, applicationScope) { SplashStore(runtime, applicationScope) }
    val splashState by splashStore.state.collectAsStateWithLifecycle()
    val firstRenderStartedAt = remember { clock.nowEpochMilliseconds() }

    val destinationContent = remember(splashStore) {
        NavigationDestinationContent { destination ->
            when (destination) {
                SplashDestination -> SplashScreen(
                    state = splashState,
                    onRetry = { splashStore.dispatch(SplashIntent.Retry) },
                )

                ReferenceDestination -> Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("Reference SDUI slice")
                }

                else -> error("No composition content registered for ${destination.navigationId}.")
            }
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
            navigationStore.dispatch(NavigationCommand.ResetTo(ReferenceDestination))
        }
    }

    DisposableEffect(syncActivationCoordinator, applicationScope) {
        val activationJob = syncActivationCoordinator.start(applicationScope)
        onDispose { activationJob.cancel() }
    }

    DisposableEffect(splashStore) {
        onDispose { splashStore.close() }
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
