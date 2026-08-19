package com.carbroz.partner.sdui.render.renderer.component

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.runtime.Composable
import com.carbroz.partner.sdui.engine.model.LayoutAxis
import com.carbroz.partner.sdui.engine.model.SduiComponent
import com.carbroz.partner.sdui.render.resolver.LayoutResolver
import com.carbroz.partner.sdui.render.scope.RenderScope

public object DefaultComponent : ComponentRenderer {
    @Composable
    override fun render(component: SduiComponent, scope: RenderScope) {
        if (!component.visible) return

        val widthMod = LayoutResolver.resolveWidth(component.width)
        val heightMod = LayoutResolver.resolveHeight(component.height)
        val marginMod = LayoutResolver.resolveMargin(widthMod.then(heightMod), component.margin)
        val paddingMod = LayoutResolver.resolvePadding(marginMod, component.padding)

        if (component.axis == LayoutAxis.HORIZONTAL) {
            val horizontalAlignment = LayoutResolver.resolveRowAlignment(component.alignment)
            val horizontalArrangement = LayoutResolver.resolveRowArrangement(component.arrangement, component.gap)
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
            val verticalAlignment = LayoutResolver.resolveColumnAlignment(component.alignment)
            val verticalArrangement = LayoutResolver.resolveColumnArrangement(component.arrangement, component.gap)
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
