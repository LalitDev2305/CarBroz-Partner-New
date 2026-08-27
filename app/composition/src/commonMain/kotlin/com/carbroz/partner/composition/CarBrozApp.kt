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
import com.carbroz.feature.dynamic.BackgroundActionExecutor
import com.carbroz.feature.dynamic.CapabilityActionExecutor
import com.carbroz.feature.dynamic.DynamicBindingContextFactory
import com.carbroz.feature.dynamic.DynamicDestination
import com.carbroz.feature.dynamic.DynamicFeatureStore
import com.carbroz.feature.dynamic.DynamicScreen
import com.carbroz.feature.dynamic.DynamicScreenCache
import com.carbroz.feature.dynamic.DynamicSduiRuntime
import com.carbroz.feature.dynamic.NetworkActionExecutor
import com.carbroz.feature.splash.BootstrapDestinationStore
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
import com.carbroz.runtime.sdui.template.form.runtime.FormTemplateRuntimeFactory
import kotlinx.coroutines.flow.collectLatest
import org.koin.compose.koinInject

/** App composition selects feature roots; dynamic-screen behavior belongs to feature:dynamic. */
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
    val formRuntime = koinInject<FormTemplateRuntimeFactory>()
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
        formRuntime,
        screenCache,
        scope,
    ) {
        DynamicFeatureStore(
            scope = scope,
            runtime = dynamicRuntime,
            actionPreparer = dynamicRuntime.actions,
            networkActions = networkActions,
            capabilityActions = capabilityActions,
            navigation = navigationStore,
            bindingContexts = bindingContexts,
            formRuntime = formRuntime,
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
                        message = "Splash bootstrap completed without a dynamic destination.",
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
                DynamicScreen(dynamicState, dynamicRuntime, dynamicStore)
            }
            else -> UnsupportedDestination(destination.navigationId)
        }
    }
}

@Composable
private fun UnsupportedDestination(navigationId: String) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text("Unsupported destination: $navigationId")
    }
}
