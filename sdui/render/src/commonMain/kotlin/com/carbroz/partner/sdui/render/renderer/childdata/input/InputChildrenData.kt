package com.carbroz.partner.sdui.render.renderer.childdata.input

import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import com.carbroz.partner.sdui.engine.model.SduiChildrenData
import com.carbroz.partner.sdui.render.renderer.childdata.ChildrenDataRenderer
import com.carbroz.partner.sdui.render.resolver.LayoutResolver
import com.carbroz.partner.sdui.render.runtime.event.SduiUiEvent
import com.carbroz.partner.sdui.render.runtime.event.SduiUiEventSink
import com.carbroz.partner.sdui.render.runtime.snapshot.SduiRenderSnapshot

public object InputChildrenData : ChildrenDataRenderer {
    @Composable
    override fun render(
        childrenData: SduiChildrenData,
        snapshot: SduiRenderSnapshot,
        eventSink: SduiUiEventSink
    ) {
        val props = InputChildrenDataProperties.decode(childrenData.properties)
        val currentValue = snapshot.getInputValue(childrenData.id) ?: ""
        val isEnabled = snapshot.isNodeEnabled(childrenData.id, childrenData.enabled)

        val widthMod = LayoutResolver.resolveWidth(childrenData.width)
        val heightMod = LayoutResolver.resolveHeight(childrenData.height)
        val marginMod = LayoutResolver.resolveMargin(widthMod.then(heightMod), childrenData.margin)
        val paddingMod = LayoutResolver.resolvePadding(marginMod, childrenData.padding)

        OutlinedTextField(
            value = currentValue,
            onValueChange = { newValue ->
                eventSink.onUiEvent(SduiUiEvent.InputChanged(childrenData.id, newValue))
            },
            enabled = isEnabled,
            label = if (props.label.isNotBlank()) { { Text(props.label) } } else null,
            placeholder = if (props.placeholder.isNotBlank()) { { Text(props.placeholder) } } else null,
            modifier = paddingMod
        )
    }
}
