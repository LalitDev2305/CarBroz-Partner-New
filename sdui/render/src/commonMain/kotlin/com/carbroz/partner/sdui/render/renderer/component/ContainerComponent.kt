package com.carbroz.partner.sdui.render.renderer.component

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.runtime.Composable
import com.carbroz.partner.sdui.engine.model.LayoutAxis
import com.carbroz.partner.sdui.engine.model.SduiComponent
import com.carbroz.partner.sdui.render.fallback.UnsupportedFallback
import com.carbroz.partner.sdui.render.resolver.LayoutResolver
import com.carbroz.partner.sdui.render.scope.RenderScope

public object ContainerComponent : ComponentRenderer {
    @Composable
    override fun render(component: SduiComponent, scope: RenderScope) {
        if (!component.visible) return

        val widthRes = LayoutResolver.resolveWidth(component.width, scope.resolutionContext)
        val heightRes = LayoutResolver.resolveHeight(component.height, scope.resolutionContext)

        if (widthRes is LayoutResolver.LayoutModifierResult.UnsupportedToken ||
            heightRes is LayoutResolver.LayoutModifierResult.UnsupportedToken
        ) {
            UnsupportedFallback.renderUnsupportedComponent(component, scope)
            return
        }

        val widthMod = (widthRes as LayoutResolver.LayoutModifierResult.Resolved).modifier
        val heightMod = (heightRes as LayoutResolver.LayoutModifierResult.Resolved).modifier
        val marginMod = LayoutResolver.resolveMargin(widthMod.then(heightMod), component.margin)
        val paddingMod = LayoutResolver.resolvePadding(marginMod, component.padding)

        if (component.axis == LayoutAxis.HORIZONTAL) {
            val arrangementRes = LayoutResolver.resolveRowArrangement(component.arrangement, component.gap, scope.resolutionContext)
            if (arrangementRes is LayoutResolver.LayoutArrangementResult.Horizontal.UnsupportedToken) {
                UnsupportedFallback.renderUnsupportedComponent(component, scope)
                return
            }
            val horizontalAlignment = LayoutResolver.resolveRowAlignment(component.alignment)
            val horizontalArrangement = (arrangementRes as LayoutResolver.LayoutArrangementResult.Horizontal.Resolved).arrangement
            Row(
                modifier = paddingMod,
                horizontalArrangement = horizontalArrangement,
                verticalAlignment = horizontalAlignment
            ) {
                for (sub in component.subcomponents) {
                    scope.renderSubComponent(sub)
                }
                for (cd in component.childrenData) {
                    scope.renderChildrenData(cd)
                }
            }
        } else {
            val arrangementRes = LayoutResolver.resolveColumnArrangement(component.arrangement, component.gap, scope.resolutionContext)
            if (arrangementRes is LayoutResolver.LayoutArrangementResult.Vertical.UnsupportedToken) {
                UnsupportedFallback.renderUnsupportedComponent(component, scope)
                return
            }
            val verticalAlignment = LayoutResolver.resolveColumnAlignment(component.alignment)
            val verticalArrangement = (arrangementRes as LayoutResolver.LayoutArrangementResult.Vertical.Resolved).arrangement
            Column(
                modifier = paddingMod,
                verticalArrangement = verticalArrangement,
                horizontalAlignment = verticalAlignment
            ) {
                for (sub in component.subcomponents) {
                    scope.renderSubComponent(sub)
                }
                for (cd in component.childrenData) {
                    scope.renderChildrenData(cd)
                }
            }
        }
    }
}
