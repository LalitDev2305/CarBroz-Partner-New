package com.carbroz.partner.sdui.render.renderer.childdata.text

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import com.carbroz.partner.sdui.engine.model.SduiChildrenData
import com.carbroz.partner.sdui.render.renderer.childdata.ChildrenDataRenderer
import com.carbroz.partner.sdui.render.renderer.theme.SduiColorAdapter
import com.carbroz.partner.sdui.render.renderer.theme.SduiTypographyAdapter
import com.carbroz.partner.sdui.render.resolver.LayoutResolver
import com.carbroz.partner.sdui.render.runtime.event.SduiUiEventSink
import com.carbroz.partner.sdui.render.runtime.snapshot.SduiRenderSnapshot

public object TextChildrenData : ChildrenDataRenderer {
    @Composable
    override fun render(
        childrenData: SduiChildrenData,
        snapshot: SduiRenderSnapshot,
        eventSink: SduiUiEventSink
    ) {
        val props = TextChildrenDataProperties.decode(childrenData.properties)
        val textStyle = SduiTypographyAdapter.parseTypography(props.styleKey)
        val textColor = SduiColorAdapter.parseColor(props.colorString, textStyle.color)

        val widthMod = LayoutResolver.resolveWidth(childrenData.width)
        val heightMod = LayoutResolver.resolveHeight(childrenData.height)
        val marginMod = LayoutResolver.resolveMargin(widthMod.then(heightMod), childrenData.margin)
        val paddingMod = LayoutResolver.resolvePadding(marginMod, childrenData.padding)

        Text(
            text = props.text,
            modifier = paddingMod,
            style = textStyle.copy(color = textColor)
        )
    }
}
