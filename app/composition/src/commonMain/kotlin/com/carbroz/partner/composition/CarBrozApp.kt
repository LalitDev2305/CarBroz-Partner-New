package com.carbroz.partner.composition

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.unit.dp
import com.carbroz.feature.splash.SplashDestination
import com.carbroz.feature.splash.SplashEffect
import com.carbroz.feature.splash.SplashScreen
import com.carbroz.feature.splash.SplashStore
import com.carbroz.foundation.lifecycle.AppLifecycle
import com.carbroz.foundation.navigation.NavigationCommand
import com.carbroz.foundation.navigation.NavigationStore
import com.carbroz.foundation.observability.Observability
import com.carbroz.foundation.time.Clock
import com.carbroz.runtime.application.ApplicationRuntime
import com.carbroz.runtime.sdui.rendering.SduiCommandIntent
import org.koin.compose.koinInject

/** Splash is static; every post-bootstrap screen is a generic backend-driven destination. */
@Composable
fun CarBrozApp() {
    val navigationStore = koinInject<NavigationStore>()
    val syncActivationCoordinator = koinInject<SyncActivationCoordinator>()
    val observability = koinInject<Observability>()
    val clock = koinInject<Clock>()
    val applicationRuntime = koinInject<ApplicationRuntime>()
    val lifecycle = koinInject<AppLifecycle>()
    val bootstrapDestinations = koinInject<BootstrapRouteStore>()
    val networkActions = koinInject<NetworkActionExecutor>()
    val capabilityActions = koinInject<CapabilityActionExecutor>()
    val dynamicRuntime = koinInject<ReferenceSduiRuntime>()
    val scope = rememberCoroutineScope()
    val splashStore = remember(applicationRuntime, lifecycle, observability, clock) {
        SplashStore(scope, applicationRuntime, lifecycle, observability, clock)
    }
    val dynamicStore = remember(dynamicRuntime, networkActions, capabilityActions, navigationStore) {
        DynamicSduiStore(scope, dynamicRuntime, dynamicRuntime.actions, networkActions, capabilityActions, navigationStore)
    }
    val navigationState by navigationStore.state.collectAsState()
    val dynamicState by dynamicStore.state.collectAsState()

    DisposableEffect(syncActivationCoordinator) { syncActivationCoordinator.start(); onDispose { syncActivationCoordinator.stop() } }
    DisposableEffect(splashStore, dynamicStore) { onDispose { splashStore.close(); dynamicStore.close() } }

    MaterialTheme {
        when (val destination = navigationState.current) {
            SplashDestination -> SplashScreen(
                store = splashStore,
                onEffect = { effect ->
                    if (effect is SplashEffect.StartupCompleted) {
                        val instruction = bootstrapDestinations.current()
                        if (instruction == null) {
                            observability.recordNonFatal(IllegalStateException("Bootstrap completed without a dynamic destination."), mapOf("component" to "app-composition"))
                        } else {
                            navigationStore.dispatch(NavigationCommand.ResetTo(DynamicDestination(instruction)))
                        }
                    }
                },
            )
            is DynamicDestination -> {
                LaunchedEffect(destination.navigationId) { dynamicStore.show(destination) }
                DynamicScreenContent(dynamicState, dynamicRuntime, dynamicStore)
            }
            else -> UnsupportedDestination(destination.navigationId)
        }
    }
}

@Composable
private fun DynamicScreenContent(state: DynamicScreenState, runtime: ReferenceSduiRuntime, store: DynamicSduiStore) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        when {
            state.loading -> CircularProgressIndicator()
            state.failure != null -> Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Unable to load dynamic screen")
                Text(state.failure.toString(), style = MaterialTheme.typography.bodySmall)
                Button(onClick = store::retry) { Text("Retry") }
            }
            state.screen != null -> runtime.renderer.RenderScreen(screen = state.screen, onCommand = { command: SduiCommandIntent -> store.onCommand(command.command) })
        }
        if (state.actionInFlight) CircularProgressIndicator()
    }
}

@Composable
private fun UnsupportedDestination(navigationId: String) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("Unsupported destination: $navigationId") }
}
