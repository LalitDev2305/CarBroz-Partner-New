package com.carbroz.partner.sdui.render.renderer.template

import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import com.carbroz.partner.sdui.engine.model.LayoutAxis
import com.carbroz.partner.sdui.engine.model.SduiTemplate
import com.carbroz.partner.sdui.render.resolver.LayoutResolver
import com.carbroz.partner.sdui.render.scope.RenderScope

public object FormTemplate : TemplateRenderer {
    @Composable
    override fun render(template: SduiTemplate, scope: RenderScope) {
        val widthMod = LayoutResolver.resolveWidth(template.width)
        val heightMod = LayoutResolver.resolveHeight(template.height)
        val marginMod = LayoutResolver.resolveMargin(widthMod.then(heightMod), template.margin)
        val paddingMod = LayoutResolver.resolvePadding(marginMod, template.padding)

        val verticalAlignment = LayoutResolver.resolveColumnAlignment(template.alignment)
        val verticalArrangement = LayoutResolver.resolveColumnArrangement(template.arrangement, template.gap)

        Column(
            modifier = paddingMod,
            verticalArrangement = verticalArrangement,
            horizontalAlignment = verticalAlignment
        ) {
            for (comp in template.components) {
                scope.renderComponent(comp)
            }
        }
    }
}
