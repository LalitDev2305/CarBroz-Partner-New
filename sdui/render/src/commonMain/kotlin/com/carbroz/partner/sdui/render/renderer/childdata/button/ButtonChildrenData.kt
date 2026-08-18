package com.carbroz.partner.sdui.render.renderer.childdata.button

import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import com.carbroz.partner.sdui.engine.model.SduiChildrenData
import com.carbroz.partner.sdui.render.renderer.childdata.ChildrenDataRenderer
import com.carbroz.partner.sdui.render.resolver.LayoutResolver
import com.carbroz.partner.sdui.render.runtime.event.SduiUiEvent
import com.carbroz.partner.sdui.render.runtime.event.SduiUiEventSink
import com.carbroz.partner.sdui.render.runtime.snapshot.SduiRenderSnapshot

public object ButtonChildrenData : ChildrenDataRenderer {
    @Composable
    override fun render(
        childrenData: SduiChildrenData,
        snapshot: SduiRenderSnapshot,
        eventSink: SduiUiEventSink
    ) {
        val props = ButtonChildrenDataProperties.decode(childrenData.properties)
        val isEnabled = snapshot.isNodeEnabled(childrenData.id, childrenData.enabled)

        val widthMod = LayoutResolver.resolveWidth(childrenData.width)
        val heightMod = LayoutResolver.resolveHeight(childrenData.height)
        val marginMod = LayoutResolver.resolveMargin(widthMod.then(heightMod), childrenData.margin)
        val paddingMod = LayoutResolver.resolvePadding(marginMod, childrenData.padding)

        Button(
            onClick = {
                eventSink.onUiEvent(
                    SduiUiEvent.NodeTriggered(
                        node = childrenData,
                        action = childrenData.action,
                        parentAction = childrenData.parentAction
                    )
                )
            },
            enabled = isEnabled,
            modifier = paddingMod
        ) {
            Text(text = props.text)
        }
    }
}
