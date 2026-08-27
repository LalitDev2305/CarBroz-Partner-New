package com.carbroz.feature.dynamic

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import com.carbroz.foundation.lifecycle.AppLifecycle
import com.carbroz.foundation.lifecycle.AppLifecycleState
import com.carbroz.foundation.navigation.NavigationStore
import com.carbroz.runtime.sdui.template.form.runtime.FormTemplateRuntimeFactory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.collectLatest

/** Process-provided dependencies for the one generic post-Splash feature. */
class DynamicFeatureFactory(
    private val runtime: DynamicSduiRuntime,
    private val networkActions: NetworkActionExecutor,
    private val capabilityActions: CapabilityActionExecutor,
    private val backgroundActions: BackgroundActionExecutor,
    private val navigation: NavigationStore,
    private val bindingContexts: DynamicBindingContextFactory,
    private val formRuntime: FormTemplateRuntimeFactory,
    private val cache: DynamicScreenCache,
) {
    fun create(scope: CoroutineScope): DynamicFeatureStore = DynamicFeatureStore(
        scope = scope,
        runtime = runtime,
        actionPreparer = runtime.actions,
        networkActions = networkActions,
        capabilityActions = capabilityActions,
        navigation = navigation,
        bindingContexts = bindingContexts,
        formRuntime = formRuntime,
        backgroundActions = backgroundActions,
        cache = cache,
    )

    internal fun runtime(): DynamicSduiRuntime = runtime
}

/**
 * Single feature host for every backend-driven screen. It owns loading, screen lifecycle,
 * background suspension, SDUI rendering and dynamic action dispatch after Splash handoff.
 */
@Composable
fun DynamicFeature(
    destination: DynamicDestination,
    factory: DynamicFeatureFactory,
    lifecycle: AppLifecycle,
) {
    val scope = rememberCoroutineScope()
    val store = remember(factory, scope) { factory.create(scope) }
    val state by store.state.collectAsState()

    DisposableEffect(store) {
        onDispose { store.close() }
    }

    LaunchedEffect(lifecycle, store) {
        lifecycle.state.collectLatest { lifecycleState ->
            if (lifecycleState == AppLifecycleState.Background) store.suspendForBackground()
        }
    }

    LaunchedEffect(destination.navigationId) {
        store.show(destination)
    }

    DynamicScreen(
        state = state,
        runtime = factory.runtime(),
        store = store,
    )
}
