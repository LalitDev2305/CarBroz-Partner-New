package com.carbroz.partner.sdui.render.renderer.childdata.button

import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import com.carbroz.partner.core.ui.adaptive.result.ResolutionResult
import com.carbroz.partner.sdui.engine.model.SduiChildrenData
import com.carbroz.partner.sdui.render.fallback.UnsupportedFallback
import com.carbroz.partner.sdui.render.renderer.childdata.ChildrenDataRenderer
import com.carbroz.partner.sdui.render.resolver.LayoutResolver
import com.carbroz.partner.sdui.render.runtime.event.SduiUiEvent
import com.carbroz.partner.sdui.render.scope.RenderScope

public object ButtonChildrenData : ChildrenDataRenderer {
    @Composable
    override fun render(
        childrenData: SduiChildrenData,
        scope: RenderScope
    ) {
        val props = ButtonChildrenDataProperties.decode(childrenData.properties)
        val isEnabled = scope.snapshot.isNodeEnabled(childrenData.id, childrenData.enabled)

        val widthRes = LayoutResolver.resolveWidth(childrenData.width, scope.resolutionContext)
        val heightRes = LayoutResolver.resolveHeight(childrenData.height, scope.resolutionContext)

        if (widthRes !is ResolutionResult.Resolved || heightRes !is ResolutionResult.Resolved) {
            UnsupportedFallback.renderUnsupportedChildrenData(childrenData.childrenDataType)
            return
        }

        val marginMod = LayoutResolver.resolveMargin(widthRes.value.then(heightRes.value), childrenData.margin)
        val paddingMod = LayoutResolver.resolvePadding(marginMod, childrenData.padding)

        Button(
            onClick = {
                scope.eventSink.onUiEvent(
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
