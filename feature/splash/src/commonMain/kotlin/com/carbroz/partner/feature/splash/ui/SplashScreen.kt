package com.carbroz.partner.feature.splash.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.carbroz.partner.feature.splash.store.SplashEffect
import com.carbroz.partner.feature.splash.store.SplashIntent
import com.carbroz.partner.feature.splash.store.SplashState
import com.carbroz.partner.feature.splash.store.SplashStore
import kotlinx.coroutines.launch

/**
 * Native Splash Screen composable.
 *
 * Renders native splash presentation and dispatches [SplashIntent.Initialize] on launch.
 */
@Composable
public fun SplashScreen(
    store: SplashStore,
    onEffect: (SplashEffect) -> Unit,
    modifier: Modifier = Modifier
) {
    val state by store.state.collectAsState()
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        scope.launch {
            store.dispatch(SplashIntent.Initialize)
        }
    }

    LaunchedEffect(store) {
        store.effects.collect { effect ->
            onEffect(effect)
        }
    }

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        when (val currentState = state) {
            is SplashState.Initial, is SplashState.Loading -> {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "CarBroz Partner",
                        style = MaterialTheme.typography.headlineMedium
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    CircularProgressIndicator()
                }
            }
            is SplashState.Success -> {
                Text(
                    text = "Initializing Application...",
                    style = MaterialTheme.typography.bodyLarge
                )
            }
            is SplashState.Error -> {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = currentState.message,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(onClick = {
                        scope.launch {
                            store.dispatch(SplashIntent.Retry)
                        }
                    }) {
                        Text("Retry")
                    }
                }
            }
        }
    }
}
