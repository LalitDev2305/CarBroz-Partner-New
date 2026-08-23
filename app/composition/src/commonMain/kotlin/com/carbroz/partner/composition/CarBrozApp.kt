package com.carbroz.partner.composition

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.carbroz.foundation.adaptive.AdaptiveLayoutProvider
import com.carbroz.foundation.designsystem.CarBrozTheme
import com.carbroz.foundation.navigation.Navigation3Host
import com.carbroz.foundation.navigation.NavigationCommand
import com.carbroz.foundation.navigation.NavigationDestination
import com.carbroz.foundation.navigation.NavigationDestinationContent
import com.carbroz.foundation.navigation.NavigationReducer
import com.carbroz.foundation.navigation.NavigationState
import com.carbroz.foundation.navigation.NavigationTransition

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
 * Navigation state is application-owned and reduced through the framework-free
 * navigation kernel. Navigation 3 only presents that canonical state; it does
 * not own a second CarBroz back stack. The temporary shell destination will be
 * replaced by the static splash/reference vertical slice in its planned phase.
 */
@Composable
fun CarBrozApp() {
    var navigationState by remember {
        mutableStateOf(NavigationState(listOf(AppShellDestination)))
    }

    fun dispatch(command: NavigationCommand) {
        when (val transition = NavigationReducer.reduce(navigationState, command)) {
            is NavigationTransition.Applied -> navigationState = transition.state
            is NavigationTransition.Ignored -> Unit
        }
    }

    CarBrozTheme {
        AdaptiveLayoutProvider {
            Navigation3Host(
                state = navigationState,
                destinationContent = appShellContent,
                onCommand = ::dispatch,
            )
        }
    }
}
