package com.carbroz.partner.sdui.render.renderer.child

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.runtime.Composable
import com.carbroz.partner.sdui.engine.model.LayoutAxis
import com.carbroz.partner.sdui.engine.model.SduiChild
import com.carbroz.partner.sdui.render.fallback.UnsupportedFallback
import com.carbroz.partner.sdui.render.resolver.LayoutResolver
import com.carbroz.partner.sdui.render.scope.RenderScope

public object ContainerChild : ChildRenderer {
    @Composable
    override fun render(child: SduiChild, scope: RenderScope) {
        if (!child.visible) return

        val widthRes = LayoutResolver.resolveWidth(child.width, scope.resolutionContext)
        val heightRes = LayoutResolver.resolveHeight(child.height, scope.resolutionContext)

        if (widthRes is LayoutResolver.LayoutModifierResult.UnsupportedToken ||
            heightRes is LayoutResolver.LayoutModifierResult.UnsupportedToken
        ) {
            UnsupportedFallback.renderUnsupportedChild(child, scope)
            return
        }

        val widthMod = (widthRes as LayoutResolver.LayoutModifierResult.Resolved).modifier
        val heightMod = (heightRes as LayoutResolver.LayoutModifierResult.Resolved).modifier
        val marginMod = LayoutResolver.resolveMargin(widthMod.then(heightMod), child.margin)
        val paddingMod = LayoutResolver.resolvePadding(marginMod, child.padding)

        if (child.axis == LayoutAxis.HORIZONTAL) {
            val arrangementRes = LayoutResolver.resolveRowArrangement(child.arrangement, child.gap, scope.resolutionContext)
            if (arrangementRes is LayoutResolver.LayoutArrangementResult.Horizontal.UnsupportedToken) {
                UnsupportedFallback.renderUnsupportedChild(child, scope)
                return
            }
            val horizontalAlignment = LayoutResolver.resolveRowAlignment(child.alignment)
            val horizontalArrangement = (arrangementRes as LayoutResolver.LayoutArrangementResult.Horizontal.Resolved).arrangement
            Row(
                modifier = paddingMod,
                horizontalArrangement = horizontalArrangement,
                verticalAlignment = horizontalAlignment
            ) {
                for (cd in child.childrenData) {
                    scope.renderChildrenData(cd)
                }
            }
        } else {
            val arrangementRes = LayoutResolver.resolveColumnArrangement(child.arrangement, child.gap, scope.resolutionContext)
            if (arrangementRes is LayoutResolver.LayoutArrangementResult.Vertical.UnsupportedToken) {
                UnsupportedFallback.renderUnsupportedChild(child, scope)
                return
            }
            val verticalAlignment = LayoutResolver.resolveColumnAlignment(child.alignment)
            val verticalArrangement = (arrangementRes as LayoutResolver.LayoutArrangementResult.Vertical.Resolved).arrangement
            Column(
                modifier = paddingMod,
                verticalArrangement = verticalArrangement,
                horizontalAlignment = verticalAlignment
            ) {
                for (cd in child.childrenData) {
                    scope.renderChildrenData(cd)
                }
            }
        }
    }
}
