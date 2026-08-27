package com.carbroz.partner.composition

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.carbroz.feature.dynamic.DynamicDestination
import com.carbroz.feature.dynamic.DynamicFeature
import com.carbroz.feature.dynamic.DynamicFeatureFactory
import com.carbroz.feature.splash.BootstrapDestinationStore
import com.carbroz.feature.splash.SplashDestination
import com.carbroz.feature.splash.SplashIntent
import com.carbroz.feature.splash.SplashScreen
import com.carbroz.feature.splash.SplashStore
import com.carbroz.foundation.lifecycle.AppLifecycle
import com.carbroz.foundation.navigation.Navigation3Host
import com.carbroz.foundation.navigation.NavigationCommand
import com.carbroz.foundation.navigation.NavigationDestination
import com.carbroz.foundation.navigation.NavigationDestinationContent
import com.carbroz.foundation.navigation.NavigationStore
import com.carbroz.foundation.observability.Observability
import com.carbroz.runtime.application.ApplicationRuntime
import kotlinx.coroutines.flow.collectLatest
import org.koin.compose.koinInject

/** Composition wires feature content into the canonical navigation presentation adapter. */
@Composable
fun CarBrozApp() {
    val navigationStore = koinInject<NavigationStore>()
    val syncActivationCoordinator = koinInject<SyncActivationCoordinator>()
    val observability = koinInject<Observability>()
    val applicationRuntime = koinInject<ApplicationRuntime>()
    val lifecycle = koinInject<AppLifecycle>()
    val bootstrapDestinations = koinInject<BootstrapDestinationStore>()
    val dynamicFeatureFactory = koinInject<DynamicFeatureFactory>()
    val scope = rememberCoroutineScope()

    val splashStore = remember(applicationRuntime, scope) { SplashStore(applicationRuntime, scope) }
    val navigationState by navigationStore.state.collectAsState()
    val splashState by splashStore.state.collectAsState()

    DisposableEffect(syncActivationCoordinator, scope) {
        val job = syncActivationCoordinator.start(scope)
        onDispose { job.cancel() }
    }
    DisposableEffect(splashStore) {
        onDispose { splashStore.close() }
    }

    LaunchedEffect(lifecycle, splashStore) {
        lifecycle.state.collectLatest { state ->
            splashStore.dispatch(SplashIntent.LifecycleChanged(state))
        }
    }
    LaunchedEffect(Unit) { splashStore.dispatch(SplashIntent.Start) }
    LaunchedEffect(splashState.isReady) {
        if (splashState.isReady && navigationStore.state.value.current == SplashDestination) {
            val instruction = bootstrapDestinations.current()
            if (instruction == null) {
                observability.crash(
                    com.carbroz.foundation.observability.CrashEvent(
                        category = "app-composition",
                        message = "Splash bootstrap completed without a dynamic destination.",
                    ),
                )
            } else {
                navigationStore.dispatch(NavigationCommand.ResetTo(DynamicDestination(instruction)))
            }
        }
    }

    MaterialTheme {
        Navigation3Host(
            state = navigationState,
            destinationContent = NavigationDestinationContent { destination ->
                DestinationContent(
                    destination = destination,
                    splashState = splashState,
                    onSplashRetry = { splashStore.dispatch(SplashIntent.Retry) },
                    dynamicFeatureFactory = dynamicFeatureFactory,
                    lifecycle = lifecycle,
                )
            },
            onCommand = navigationStore::dispatch,
        )
    }
}

@Composable
private fun DestinationContent(
    destination: NavigationDestination,
    splashState: com.carbroz.feature.splash.SplashState,
    onSplashRetry: () -> Unit,
    dynamicFeatureFactory: DynamicFeatureFactory,
    lifecycle: AppLifecycle,
) {
    when (destination) {
        SplashDestination -> SplashScreen(state = splashState, onRetry = onSplashRetry)
        is DynamicDestination -> DynamicFeature(
            destination = destination,
            factory = dynamicFeatureFactory,
            lifecycle = lifecycle,
        )
        else -> UnsupportedDestination(destination.navigationId)
    }
}

@Composable
private fun UnsupportedDestination(navigationId: String) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text("Unsupported destination: $navigationId")
    }
}
