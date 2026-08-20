package com.carbroz.partner.sdui.render.renderer.childdata.input

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import com.carbroz.partner.core.ui.adaptive.result.ResolutionResult
import com.carbroz.partner.sdui.engine.model.SduiChildrenData
import com.carbroz.partner.sdui.render.fallback.UnsupportedFallback
import com.carbroz.partner.sdui.render.renderer.childdata.ChildrenDataRenderer
import com.carbroz.partner.sdui.render.resolver.LayoutResolver
import com.carbroz.partner.sdui.render.runtime.event.SduiUiEvent
import com.carbroz.partner.sdui.render.scope.RenderScope

public object InputChildrenData : ChildrenDataRenderer {
    @Composable
    override fun render(
        childrenData: SduiChildrenData,
        scope: RenderScope
    ) {
        val props = InputChildrenDataProperties.decode(childrenData.properties)
        val currentValue = scope.snapshot.getNodeValue(childrenData.id) ?: ""
        val validationError = scope.snapshot.getValidationError(childrenData.id)
        val isEnabled = scope.snapshot.isNodeEnabled(childrenData.id, childrenData.enabled)

        val widthRes = LayoutResolver.resolveWidth(childrenData.width, scope.resolutionContext)
        val heightRes = LayoutResolver.resolveHeight(childrenData.height, scope.resolutionContext)

        if (widthRes !is ResolutionResult.Resolved || heightRes !is ResolutionResult.Resolved) {
            UnsupportedFallback.renderUnsupportedChildrenData(childrenData.childrenDataType)
            return
        }

        val marginMod = LayoutResolver.resolveMargin(widthRes.value.then(heightRes.value), childrenData.margin)
        val paddingMod = LayoutResolver.resolvePadding(marginMod, childrenData.padding)

        OutlinedTextField(
            value = currentValue,
            onValueChange = { newValue ->
                scope.eventSink.onUiEvent(SduiUiEvent.ValueChanged(childrenData.id, newValue))
            },
            enabled = isEnabled,
            isError = validationError != null,
            label = if (props.label.isNotBlank()) { { Text(props.label) } } else null,
            placeholder = if (props.placeholder.isNotBlank()) { { Text(props.placeholder) } } else null,
            supportingText = if (validationError != null) {
                { Text(text = validationError, color = MaterialTheme.colorScheme.error) }
            } else null,
            modifier = paddingMod
        )
    }
}
