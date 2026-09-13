package com.carbroz.feature.dynamic

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import com.carbroz.data.network.NetworkDataSource
import com.carbroz.foundation.lifecycle.AppLifecycle
import com.carbroz.foundation.lifecycle.AppLifecycleState
import com.carbroz.foundation.navigation.NavigationStore
import com.carbroz.sdui.parser.SduiDecoder
import com.carbroz.sdui.parser.SduiSupportChecker
import com.carbroz.sdui.render.SduiRenderer
import com.carbroz.sdui.value.SduiValueResolver
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.collectLatest

/** Process-provided construction boundary for the one generic post-Splash feature. */
class DynamicFeatureFactory(
    private val decoder: SduiDecoder,
    private val supportChecker: SduiSupportChecker,
    private val renderer: SduiRenderer,
    private val valueResolver: SduiValueResolver,
    private val network: NetworkDataSource,
    private val actionExecutor: SduiActionExecutor,
    private val contextProvider: DynamicContextProvider,
    private val flowContext: DynamicFlowContext,
    private val navigation: NavigationStore,
    private val resolveAssetUrl: (String) -> String,
) {
    fun create(scope: CoroutineScope): DynamicScreenStore = DynamicScreenStore(
        scope = scope,
        decoder = decoder,
        supportChecker = supportChecker,
        network = network,
        actionExecutor = actionExecutor,
        contextProvider = contextProvider,
        flowContext = flowContext,
        navigation = navigation,
    )

    internal fun renderer(): SduiRenderer = renderer
    internal fun valueResolver(): SduiValueResolver = valueResolver
    internal fun resolveAssetUrl(): (String) -> String = resolveAssetUrl
}

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
        store.dispatch(DynamicScreenIntent.Show(destination))
    }

    DynamicScreen(
        state = state,
        renderer = factory.renderer(),
        valueResolver = factory.valueResolver(),
        resolveAssetUrl = factory.resolveAssetUrl(),
        onIntent = store::dispatch,
    )
}
