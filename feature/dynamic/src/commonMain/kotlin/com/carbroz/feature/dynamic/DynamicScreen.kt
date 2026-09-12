package com.carbroz.feature.dynamic

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import com.carbroz.sdui.model.SduiPresentationMode
import com.carbroz.sdui.render.SduiRenderContext
import com.carbroz.sdui.render.SduiRenderer
import com.carbroz.sdui.value.SduiExecutionContext
import com.carbroz.sdui.value.SduiValueResolution
import com.carbroz.sdui.value.SduiValueResolver

@Composable
fun DynamicScreen(
    state: DynamicScreenState,
    renderer: SduiRenderer,
    valueResolver: SduiValueResolver,
    resolveAssetUrl: (String) -> String,
    onIntent: (DynamicScreenIntent) -> Unit,
) {
    val execution = SduiExecutionContext(
        bindings = state.fields.mapValues { (_, field) -> field.value },
        context = state.context,
        response = state.response,
    )
    val renderContext = SduiRenderContext(
        fields = state.fields,
        nodeStates = state.nodeStates,
        resolveValue = { raw ->
            when (val resolved = valueResolver.resolve(raw, execution)) {
                is SduiValueResolution.Success -> resolved.value
                is SduiValueResolution.Failure -> null
            }
        },
        resolveAssetUrl = resolveAssetUrl,
        onInteraction = { onIntent(DynamicScreenIntent.Interaction(it)) },
    )

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        when {
            state.loading && state.screen == null -> CircularProgressIndicator()
            state.failure != null -> DynamicFailure(
                onRetry = { onIntent(DynamicScreenIntent.Retry) },
            )
            state.screen != null -> renderer.Render(state.screen, renderContext)
        }

        if (state.actionInFlight) CircularProgressIndicator()

        val screen = state.screen
        val overlay = state.overlay
        if (screen != null && overlay != null) {
            when (overlay.presentation) {
                SduiPresentationMode.DIALOG -> AlertDialog(
                    onDismissRequest = {},
                    confirmButton = {},
                    text = { renderer.RenderTarget(screen, overlay.targetId, renderContext) },
                )

                SduiPresentationMode.BOTTOM_SHEET -> ModalBottomSheet(onDismissRequest = {}) {
                    renderer.RenderTarget(screen, overlay.targetId, renderContext)
                }

                SduiPresentationMode.POPUP -> Popup(onDismissRequest = {}) {
                    Surface { renderer.RenderTarget(screen, overlay.targetId, renderContext) }
                }
            }
        }
    }
}

@Composable
private fun DynamicFailure(onRetry: () -> Unit) {
    Column(
        modifier = Modifier.padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("Unable to load this screen")
        Button(onClick = onRetry) { Text("Retry") }
    }
}
