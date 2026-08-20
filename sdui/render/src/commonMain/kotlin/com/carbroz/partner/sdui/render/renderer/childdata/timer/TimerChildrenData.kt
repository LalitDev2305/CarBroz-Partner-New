package com.carbroz.partner.sdui.render.renderer.childdata.timer

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import com.carbroz.partner.sdui.engine.model.SduiChildrenData
import com.carbroz.partner.sdui.render.fallback.UnsupportedFallback
import com.carbroz.partner.sdui.render.renderer.childdata.ChildrenDataRenderer
import com.carbroz.partner.sdui.render.resolver.LayoutResolver
import com.carbroz.partner.sdui.render.scope.RenderScope

public object TimerChildrenData : ChildrenDataRenderer {
    @Composable
    override fun render(
        childrenData: SduiChildrenData,
        scope: RenderScope
    ) {
        val props = TimerChildrenDataProperties.decode(childrenData.properties)
        val rawNodeValue = scope.snapshot.getNodeValue(childrenData.id)?.toIntOrNull()
        val remainingSeconds = rawNodeValue ?: props.initialSeconds

        val minutes = remainingSeconds / 60
        val seconds = remainingSeconds % 60
        val formattedText = "${minutes.toString().padStart(2, '0')}:${seconds.toString().padStart(2, '0')}"

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
            text = formattedText,
            modifier = paddingMod
        )
    }
}
