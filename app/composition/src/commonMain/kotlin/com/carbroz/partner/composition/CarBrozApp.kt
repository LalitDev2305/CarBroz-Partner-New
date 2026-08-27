package com.carbroz.partner.composition

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
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
import com.carbroz.feature.splash.SplashIntent
import com.carbroz.feature.splash.SplashScreen
import com.carbroz.feature.splash.SplashStore
import com.carbroz.foundation.lifecycle.AppLifecycle
import com.carbroz.foundation.lifecycle.AppLifecycleState
import com.carbroz.foundation.navigation.NavigationCommand
import com.carbroz.foundation.navigation.NavigationStore
import com.carbroz.foundation.observability.Observability
import com.carbroz.runtime.application.ApplicationRuntime
import com.carbroz.runtime.sdui.rendering.DynamicScreenHost
import kotlinx.coroutines.flow.collectLatest
import kotlinx.serialization.json.JsonPrimitive
import org.koin.compose.koinInject

/** Splash is static; every post-bootstrap screen is a generic backend-driven destination. */
@Composable
fun CarBrozApp() {
    val navigationStore = koinInject<NavigationStore>()
    val syncActivationCoordinator = koinInject<SyncActivationCoordinator>()
    val observability = koinInject<Observability>()
    val applicationRuntime = koinInject<ApplicationRuntime>()
    val lifecycle = koinInject<AppLifecycle>()
    val bootstrapDestinations = koinInject<BootstrapDestinationStore>()
    val networkActions = koinInject<NetworkActionExecutor>()
    val capabilityActions = koinInject<CapabilityActionExecutor>()
    val backgroundActions = koinInject<BackgroundActionExecutor>()
    val dynamicRuntime = koinInject<DynamicSduiRuntime>()
    val bindingContexts = koinInject<DynamicBindingContextFactory>()
    val formStores = koinInject<DynamicFormStoreFactory>()
    val screenCache = koinInject<DynamicScreenCache>()
    val scope = rememberCoroutineScope()

    val splashStore = remember(applicationRuntime, scope) { SplashStore(applicationRuntime, scope) }
    val dynamicStore = remember(
        dynamicRuntime,
        networkActions,
        capabilityActions,
        backgroundActions,
        navigationStore,
        bindingContexts,
        formStores,
        screenCache,
        scope,
    ) {
        DynamicSduiStore(
            scope = scope,
            runtime = dynamicRuntime,
            actionPreparer = dynamicRuntime.actions,
            networkActions = networkActions,
            capabilityActions = capabilityActions,
            navigation = navigationStore,
            bindingContexts = bindingContexts,
            formStores = formStores,
            backgroundActions = backgroundActions,
            cache = screenCache,
        )
    }

    val navigationState by navigationStore.state.collectAsState()
    val splashState by splashStore.state.collectAsState()
    val dynamicState by dynamicStore.state.collectAsState()

    DisposableEffect(syncActivationCoordinator, scope) {
        val job = syncActivationCoordinator.start(scope)
        onDispose { job.cancel() }
    }
    DisposableEffect(splashStore, dynamicStore) {
        onDispose {
            splashStore.close()
            dynamicStore.close()
        }
    }

    LaunchedEffect(lifecycle, splashStore, dynamicStore) {
        lifecycle.state.collectLatest { state ->
            splashStore.dispatch(SplashIntent.LifecycleChanged(state))
            if (state == AppLifecycleState.Background) dynamicStore.suspendForBackground()
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
                        message = "Bootstrap completed without a dynamic destination.",
                    ),
                )
            } else {
                navigationStore.dispatch(NavigationCommand.ResetTo(DynamicDestination(instruction)))
            }
        }
    }

    MaterialTheme {
        when (val destination = navigationState.current) {
            SplashDestination -> SplashScreen(
                state = splashState,
                onRetry = { splashStore.dispatch(SplashIntent.Retry) },
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
private fun DynamicScreenContent(
    state: DynamicScreenState,
    runtime: DynamicSduiRuntime,
    store: DynamicSduiStore,
) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        when {
            state.loading && state.screen == null -> CircularProgressIndicator()
            state.failure != null -> Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text("Unable to load dynamic screen")
                Text(state.failure.toString(), style = MaterialTheme.typography.bodySmall)
                Button(onClick = store::retry) { Text("Retry") }
            }
            state.screen != null -> DynamicScreenHost(
                screen = state.screen,
                dispatcher = runtime.renderer,
                onCommand = store::onCommand,
                onRenderFailure = { store.onRenderFailure(it.toString()) },
                values = store.renderValues,
            )
        }

        if (state.actionInFlight) CircularProgressIndicator()
        state.presentation?.let { DynamicPresentationHost(it, store::dismissPresentation) }
    }
}

@Composable
private fun DynamicPresentationHost(
    presentation: DynamicPresentationState,
    onDismiss: () -> Unit,
) {
    val message = (presentation.properties["message"] as? JsonPrimitive)?.content ?: presentation.id
    Surface(
        modifier = Modifier.padding(24.dp).widthIn(max = 480.dp),
        tonalElevation = 6.dp,
        shadowElevation = 6.dp,
    ) {
        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(presentation.kind.name, style = MaterialTheme.typography.labelMedium)
            Text(message, style = MaterialTheme.typography.bodyLarge)
            Button(onClick = onDismiss) { Text("Dismiss") }
        }
    }
}

@Composable
private fun UnsupportedDestination(navigationId: String) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text("Unsupported destination: $navigationId")
    }
}
