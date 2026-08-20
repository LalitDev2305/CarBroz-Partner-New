package com.carbroz.partner.sdui.render.renderer.childdata.text

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import com.carbroz.partner.core.ui.adaptive.result.ResolutionResult
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

        if (widthRes !is ResolutionResult.Resolved || heightRes !is ResolutionResult.Resolved) {
            UnsupportedFallback.renderUnsupportedChildrenData(childrenData.childrenDataType)
            return
        }

        val marginMod = LayoutResolver.resolveMargin(widthRes.value.then(heightRes.value), childrenData.margin)
        val paddingMod = LayoutResolver.resolvePadding(marginMod, childrenData.padding)

        Text(
            text = props.text,
            modifier = paddingMod,
            style = textStyle.copy(color = textColor)
        )
    }
}
