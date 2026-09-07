package com.carbroz.partner.composition

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
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
import com.carbroz.feature.dynamic.DynamicScreenInstruction
import com.carbroz.feature.splash.SplashDestination
import com.carbroz.feature.splash.SplashEffect
import com.carbroz.feature.splash.SplashIntent
import com.carbroz.feature.splash.SplashScreen
import com.carbroz.feature.splash.SplashState
import com.carbroz.feature.splash.SplashStore
import com.carbroz.foundation.capabilities.CapabilityKind
import com.carbroz.foundation.capabilities.CapabilityRegistry
import com.carbroz.foundation.capabilities.GenericCapabilityRequest
import com.carbroz.foundation.designsystem.CarBrozTheme
import com.carbroz.foundation.lifecycle.AppLifecycle
import com.carbroz.foundation.navigation.Navigation3Host
import com.carbroz.foundation.navigation.NavigationDestination
import com.carbroz.foundation.navigation.NavigationDestinationContent
import com.carbroz.foundation.navigation.NavigationStore
import com.carbroz.foundation.observability.CrashEvent
import com.carbroz.foundation.observability.Observability
import com.carbroz.runtime.application.ApplicationRuntime
import kotlinx.coroutines.flow.collectLatest
import kotlinx.serialization.json.JsonPrimitive
import org.koin.compose.koinInject

/** Composition maps feature effects to canonical application owners; it owns no parallel startup state. */
@Composable
fun CarBrozApp() {
    val navigationStore = koinInject<NavigationStore>()
    val syncActivationCoordinator = koinInject<SyncActivationCoordinator>()
    val observability = koinInject<Observability>()
    val applicationRuntime = koinInject<ApplicationRuntime>()
    val lifecycle = koinInject<AppLifecycle>()
    val capabilityRegistry = koinInject<CapabilityRegistry>()
    val dynamicFeatureFactory = koinInject<DynamicFeatureFactory>()
    val scope = rememberCoroutineScope()

    val splashStore = remember(applicationRuntime, scope) {
        SplashStore(applicationRuntime, scope)
    }
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

    LaunchedEffect(splashStore) {
        splashStore.effects.collect { effect ->
            when (effect) {
                is SplashEffect.Navigate -> {
                    if (navigationStore.state.value.current != SplashDestination) return@collect
                    val instruction = effect.payload as? DynamicScreenInstruction
                    if (instruction == null) {
                        observability.crash(
                            CrashEvent(
                                category = "app-composition",
                                message = "Unsupported startup payload produced by application runtime.",
                            ),
                        )
                    } else {
                        NavigationProcessStateBridge.applyAfterBootstrap(DynamicDestination(instruction))
                    }
                }

                is SplashEffect.OpenUpdateUri -> {
                    capabilityRegistry.execute(
                        GenericCapabilityRequest(
                            kind = CapabilityKind.EXTERNAL_URI,
                            operation = "open",
                            arguments = mapOf("uri" to JsonPrimitive(effect.uri)),
                        ),
                    )
                }
            }
        }
    }

    CarBrozTheme {
        Navigation3Host(
            state = navigationState,
            destinationContent = NavigationDestinationContent { destination ->
                DestinationContent(
                    destination = destination,
                    splashState = splashState,
                    onSplashRetry = { splashStore.dispatch(SplashIntent.RetryClicked) },
                    onSplashUpdate = { splashStore.dispatch(SplashIntent.UpdateClicked) },
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
    splashState: SplashState,
    onSplashRetry: () -> Unit,
    onSplashUpdate: () -> Unit,
    dynamicFeatureFactory: DynamicFeatureFactory,
    lifecycle: AppLifecycle,
) {
    when (destination) {
        SplashDestination -> SplashScreen(
            state = splashState,
            onRetry = onSplashRetry,
            onUpdateRequested = onSplashUpdate,
        )

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
