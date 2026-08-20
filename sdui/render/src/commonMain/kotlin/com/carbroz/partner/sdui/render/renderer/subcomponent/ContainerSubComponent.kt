package com.carbroz.partner.sdui.render.renderer.subcomponent

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.runtime.Composable
import com.carbroz.partner.sdui.engine.model.LayoutAxis
import com.carbroz.partner.sdui.engine.model.SduiSubComponent
import com.carbroz.partner.sdui.render.fallback.UnsupportedFallback
import com.carbroz.partner.sdui.render.resolver.LayoutResolver
import com.carbroz.partner.sdui.render.scope.RenderScope

public object ContainerSubComponent : SubComponentRenderer {
    @Composable
    override fun render(subComponent: SduiSubComponent, scope: RenderScope) {
        if (!subComponent.visible) return

        val widthRes = LayoutResolver.resolveWidth(subComponent.width, scope.resolutionContext)
        val heightRes = LayoutResolver.resolveHeight(subComponent.height, scope.resolutionContext)

        if (widthRes is LayoutResolver.LayoutModifierResult.UnsupportedToken ||
            heightRes is LayoutResolver.LayoutModifierResult.UnsupportedToken
        ) {
            UnsupportedFallback.renderUnsupportedSubComponent(subComponent, scope)
            return
        }

        val widthMod = (widthRes as LayoutResolver.LayoutModifierResult.Resolved).modifier
        val heightMod = (heightRes as LayoutResolver.LayoutModifierResult.Resolved).modifier
        val marginMod = LayoutResolver.resolveMargin(widthMod.then(heightMod), subComponent.margin)
        val paddingMod = LayoutResolver.resolvePadding(marginMod, subComponent.padding)

        if (subComponent.axis == LayoutAxis.HORIZONTAL) {
            val arrangementRes = LayoutResolver.resolveRowArrangement(subComponent.arrangement, subComponent.gap, scope.resolutionContext)
            if (arrangementRes is LayoutResolver.LayoutArrangementResult.Horizontal.UnsupportedToken) {
                UnsupportedFallback.renderUnsupportedSubComponent(subComponent, scope)
                return
            }
            val horizontalAlignment = LayoutResolver.resolveRowAlignment(subComponent.alignment)
            val horizontalArrangement = (arrangementRes as LayoutResolver.LayoutArrangementResult.Horizontal.Resolved).arrangement
            Row(
                modifier = paddingMod,
                horizontalArrangement = horizontalArrangement,
                verticalAlignment = horizontalAlignment
            ) {
                for (ch in subComponent.children) {
                    scope.renderChild(ch)
                }
                for (cd in subComponent.childrenData) {
                    scope.renderChildrenData(cd)
                }
            }
        } else {
            val arrangementRes = LayoutResolver.resolveColumnArrangement(subComponent.arrangement, subComponent.gap, scope.resolutionContext)
            if (arrangementRes is LayoutResolver.LayoutArrangementResult.Vertical.UnsupportedToken) {
                UnsupportedFallback.renderUnsupportedSubComponent(subComponent, scope)
                return
            }
            val verticalAlignment = LayoutResolver.resolveColumnAlignment(subComponent.alignment)
            val verticalArrangement = (arrangementRes as LayoutResolver.LayoutArrangementResult.Vertical.Resolved).arrangement
            Column(
                modifier = paddingMod,
                verticalArrangement = verticalArrangement,
                horizontalAlignment = verticalAlignment
            ) {
                for (ch in subComponent.children) {
                    scope.renderChild(ch)
                }
                for (cd in subComponent.childrenData) {
                    scope.renderChildrenData(cd)
                }
            }
        }
    }
}
