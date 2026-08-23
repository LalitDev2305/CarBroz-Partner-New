package com.carbroz.partner.composition

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.carbroz.foundation.adaptive.AdaptiveLayoutProvider
import com.carbroz.foundation.designsystem.CarBrozTheme
import com.carbroz.foundation.navigation.Navigation3Host
import com.carbroz.foundation.navigation.NavigationDestination
import com.carbroz.foundation.navigation.NavigationDestinationContent
import com.carbroz.foundation.navigation.NavigationState
import com.carbroz.foundation.navigation.NavigationStore

private data object AppShellDestination : NavigationDestination {
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
 * [NavigationStore] owns the canonical semantic back stack outside transient UI
 * state. Navigation 3 only presents that state and emits semantic commands back
 * to the same store. The temporary shell destination will be replaced by the
 * static splash/reference vertical slice in its planned phase.
 */
@Composable
fun CarBrozApp() {
    val navigationStore = remember {
        NavigationStore(NavigationState(listOf(AppShellDestination)))
    }
    val navigationState by navigationStore.state.collectAsStateWithLifecycle()

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
