package com.carbroz.partner.sdui.render.renderer.child

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.runtime.Composable
import com.carbroz.partner.sdui.engine.model.LayoutAxis
import com.carbroz.partner.sdui.engine.model.SduiChild
import com.carbroz.partner.sdui.render.resolver.LayoutResolver
import com.carbroz.partner.sdui.render.scope.RenderScope

public object ContainerChild : ChildRenderer {
    @Composable
    override fun render(child: SduiChild, scope: RenderScope) {
        if (!child.visible) return

        val widthMod = LayoutResolver.resolveWidth(child.width)
        val heightMod = LayoutResolver.resolveHeight(child.height)
        val marginMod = LayoutResolver.resolveMargin(widthMod.then(heightMod), child.margin)
        val paddingMod = LayoutResolver.resolvePadding(marginMod, child.padding)

        if (child.axis == LayoutAxis.HORIZONTAL) {
            val horizontalAlignment = LayoutResolver.resolveRowAlignment(child.alignment)
            val horizontalArrangement = LayoutResolver.resolveRowArrangement(child.arrangement, child.gap)
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
            val verticalAlignment = LayoutResolver.resolveColumnAlignment(child.alignment)
            val verticalArrangement = LayoutResolver.resolveColumnArrangement(child.arrangement, child.gap)
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
