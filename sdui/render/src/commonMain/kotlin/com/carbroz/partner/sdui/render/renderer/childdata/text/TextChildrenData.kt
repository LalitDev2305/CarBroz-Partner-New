package com.carbroz.partner.sdui.render.renderer.childdata.text

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import com.carbroz.partner.sdui.engine.model.SduiChildrenData
import com.carbroz.partner.sdui.render.fallback.UnsupportedFallback
import com.carbroz.partner.sdui.render.renderer.childdata.ChildrenDataRenderer
import com.carbroz.partner.sdui.render.renderer.theme.SduiColorAdapter
import com.carbroz.partner.sdui.render.renderer.theme.SduiTypographyAdapter
import com.carbroz.partner.sdui.render.resolver.LayoutResolver
import com.carbroz.partner.sdui.render.scope.RenderScope

public object TextChildrenData : ChildrenDataRenderer {
    @Composable
    override fun render(
        childrenData: SduiChildrenData,
        scope: RenderScope
    ) {
        val props = TextChildrenDataProperties.decode(childrenData.properties)
        val textStyle = SduiTypographyAdapter.parseTypography(props.styleKey)
        val textColor = SduiColorAdapter.parseColor(props.colorString, textStyle.color)

        val widthRes = LayoutResolver.resolveWidth(childrenData.width, scope.resolutionContext)
        val heightRes = LayoutResolver.resolveHeight(childrenData.height, scope.resolutionContext)

        if (widthRes is LayoutResolver.LayoutModifierResult.UnsupportedToken ||
            heightRes is LayoutResolver.LayoutModifierResult.UnsupportedToken
        ) {
            UnsupportedFallback.renderUnsupportedChildrenData(childrenData.childrenDataType)
            return
        }

        val widthMod = (widthRes as LayoutResolver.LayoutModifierResult.Resolved).modifier
        val heightMod = (heightRes as LayoutResolver.LayoutModifierResult.Resolved).modifier
        val marginMod = LayoutResolver.resolveMargin(widthMod.then(heightMod), childrenData.margin)
        val paddingMod = LayoutResolver.resolvePadding(marginMod, childrenData.padding)

        Text(
            text = props.text,
            modifier = paddingMod,
            style = textStyle.copy(color = textColor)
        )
    }
}
