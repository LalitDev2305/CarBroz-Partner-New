package com.carbroz.partner.composition

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.carbroz.foundation.adaptive.AdaptiveLayoutProvider
import com.carbroz.foundation.designsystem.CarBrozTheme
import com.carbroz.foundation.navigation.Navigation3Host
import com.carbroz.foundation.navigation.NavigationDestination
import com.carbroz.foundation.navigation.NavigationDestinationContent
import com.carbroz.foundation.navigation.NavigationStore
import org.koin.compose.koinInject

internal data object AppShellDestination : NavigationDestination {
    override val navigationId: String = "app-shell"
}

private val appShellContent = NavigationDestinationContent { destination ->
    when (destination) {
        AppShellDestination -> Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            Text("CarBroz Partner")
        }
        else -> error("No composition content registered for ${destination.navigationId}.")
    }
}

/**
 * Application composition root.
 *
 * [NavigationStore] is application-scoped in DI and owns the canonical semantic
 * back stack independently of Compose lifecycle. Navigation 3 only presents
 * that state and emits semantic commands back to the same store. The temporary
 * shell destination will be replaced by the static splash/reference vertical
 * slice in its planned phase.
 */
@Composable
fun CarBrozApp() {
    val navigationStore = koinInject<NavigationStore>()
    val syncActivationCoordinator = koinInject<SyncActivationCoordinator>()
    val navigationState by navigationStore.state.collectAsStateWithLifecycle()
    val applicationScope = rememberCoroutineScope()

    DisposableEffect(syncActivationCoordinator, applicationScope) {
        val activationJob = syncActivationCoordinator.start(applicationScope)
        onDispose { activationJob.cancel() }
    }

    CarBrozTheme {
        AdaptiveLayoutProvider {
            Navigation3Host(
                state = navigationState,
                destinationContent = appShellContent,
                onCommand = navigationStore::dispatch,
            )
        }
    }
}
