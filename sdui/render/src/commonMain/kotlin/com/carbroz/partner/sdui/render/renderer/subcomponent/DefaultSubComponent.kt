package com.carbroz.partner.sdui.render.renderer.subcomponent

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.runtime.Composable
import com.carbroz.partner.sdui.engine.model.LayoutAxis
import com.carbroz.partner.sdui.engine.model.SduiSubComponent
import com.carbroz.partner.sdui.render.resolver.LayoutResolver
import com.carbroz.partner.sdui.render.scope.RenderScope

public object DefaultSubComponent : SubComponentRenderer {
    @Composable
    override fun render(subComponent: SduiSubComponent, scope: RenderScope) {
        if (!subComponent.visible) return

        val widthMod = LayoutResolver.resolveWidth(subComponent.width)
        val heightMod = LayoutResolver.resolveHeight(subComponent.height)
        val marginMod = LayoutResolver.resolveMargin(widthMod.then(heightMod), subComponent.margin)
        val paddingMod = LayoutResolver.resolvePadding(marginMod, subComponent.padding)

        if (subComponent.axis == LayoutAxis.HORIZONTAL) {
            val horizontalAlignment = LayoutResolver.resolveRowAlignment(subComponent.alignment)
            val horizontalArrangement = LayoutResolver.resolveRowArrangement(subComponent.arrangement, subComponent.gap)
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
            val verticalAlignment = LayoutResolver.resolveColumnAlignment(subComponent.alignment)
            val verticalArrangement = LayoutResolver.resolveColumnArrangement(subComponent.arrangement, subComponent.gap)
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
