package com.carbroz.feature.dynamic

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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.carbroz.runtime.sdui.rendering.DynamicScreenHost
import kotlinx.serialization.json.JsonPrimitive

@Composable
fun DynamicScreen(
    state: DynamicScreenState,
    runtime: DynamicSduiRuntime,
    store: DynamicFeatureStore,
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
