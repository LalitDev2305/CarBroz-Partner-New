package com.carbroz.partner.sdui.render.renderer.childdata.timer

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import com.carbroz.partner.sdui.engine.model.SduiChildrenData
import com.carbroz.partner.sdui.render.renderer.childdata.ChildrenDataRenderer
import com.carbroz.partner.sdui.render.resolver.LayoutResolver
import com.carbroz.partner.sdui.render.runtime.event.SduiUiEventSink
import com.carbroz.partner.sdui.render.runtime.snapshot.SduiRenderSnapshot

public object TimerChildrenData : ChildrenDataRenderer {
    @Composable
    override fun render(
        childrenData: SduiChildrenData,
        snapshot: SduiRenderSnapshot,
        eventSink: SduiUiEventSink
    ) {
        val props = TimerChildrenDataProperties.decode(childrenData.properties)
        val rawNodeValue = snapshot.getNodeValue(childrenData.id)?.toIntOrNull()
        val remainingSeconds = rawNodeValue ?: props.initialSeconds

        val minutes = remainingSeconds / 60
        val seconds = remainingSeconds % 60
        val formattedText = "${minutes.toString().padStart(2, '0')}:${seconds.toString().padStart(2, '0')}"

        val widthMod = LayoutResolver.resolveWidth(childrenData.width)
        val heightMod = LayoutResolver.resolveHeight(childrenData.height)
        val marginMod = LayoutResolver.resolveMargin(widthMod.then(heightMod), childrenData.margin)
        val paddingMod = LayoutResolver.resolvePadding(marginMod, childrenData.padding)

        Text(
            text = formattedText,
            modifier = paddingMod
        )
    }
}
