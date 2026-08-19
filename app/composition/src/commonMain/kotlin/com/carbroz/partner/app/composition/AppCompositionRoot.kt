package com.carbroz.partner.app.composition

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.carbroz.partner.app.composition.config.AppConfig
import com.carbroz.partner.app.composition.graph.AppGraph
import com.carbroz.partner.feature.splash.orchestrator.StartupDestination
import com.carbroz.partner.feature.splash.store.SplashEffect
import com.carbroz.partner.feature.splash.ui.SplashScreen
import com.carbroz.partner.sdui.host.composable.SduiScreenHost

/**
 * Root multiplatform Compose entry point for CarBroz Partner.
 *
 * Serves as the single multiplatform rendering root invoked by platform hosts.
 * Renders native [SplashScreen] first; upon receiving [SplashEffect.NavigateToDestination],
 * transitions application layout to [SduiScreenHost].
 */
@Composable
public fun CarBrozPartnerRoot(
    config: AppConfig = AppConfig.production(),
    modifier: Modifier = Modifier
) {
    val appGraph = remember(config) { AppGraph(config) }
    val scope = rememberCoroutineScope()

    val splashStore = remember(appGraph) { appGraph.createSplashStore(scope) }

    var currentDestination by remember { mutableStateOf<StartupDestination?>(null) }

    MaterialTheme {
        val destination = currentDestination
        if (destination == null) {
            SplashScreen(
                store = splashStore,
                onEffect = { effect ->
                    when (effect) {
                        is SplashEffect.NavigateToDestination -> {
                            currentDestination = effect.destination
                        }
                    }
                },
                modifier = modifier
            )
        } else {
            when (destination) {
                is StartupDestination.ServerDrivenUi -> {
                    val controller = remember(appGraph) {
                        appGraph.createHostController(scope)
                    }

                    androidx.compose.runtime.LaunchedEffect(appGraph.sessionStore) {
                        var previousState: com.carbroz.partner.domain.session.model.SessionState? = null
                        appGraph.sessionStore.state.collect { currentState ->
                            if (previousState == com.carbroz.partner.domain.session.model.SessionState.Authenticated &&
                                currentState == com.carbroz.partner.domain.session.model.SessionState.Unauthenticated
                            ) {
                                controller.loadScreen(appGraph.endpointConfig.entryEndpoint)
                            }
                            previousState = currentState
                        }
                    }

                    SduiScreenHost(
                        controller = controller,
                        modifier = modifier
                    )
                }
            }
        }
    }
}
