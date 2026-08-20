package com.carbroz.partner.sdui.render.renderer.template

import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import com.carbroz.partner.sdui.engine.model.SduiTemplate
import com.carbroz.partner.sdui.render.fallback.UnsupportedFallback
import com.carbroz.partner.sdui.render.resolver.LayoutResolver
import com.carbroz.partner.sdui.render.scope.RenderScope

public object FormTemplate : TemplateRenderer {
    @Composable
    override fun render(template: SduiTemplate, scope: RenderScope) {
        val widthRes = LayoutResolver.resolveWidth(template.width, scope.resolutionContext)
        val heightRes = LayoutResolver.resolveHeight(template.height, scope.resolutionContext)
        val arrangementRes = LayoutResolver.resolveColumnArrangement(template.arrangement, template.gap, scope.resolutionContext)

        if (widthRes is LayoutResolver.LayoutModifierResult.UnsupportedToken ||
            heightRes is LayoutResolver.LayoutModifierResult.UnsupportedToken ||
            arrangementRes is LayoutResolver.LayoutArrangementResult.Vertical.UnsupportedToken
        ) {
            UnsupportedFallback.renderUnsupportedTemplate(template, scope)
            return
        }

        val widthMod = (widthRes as LayoutResolver.LayoutModifierResult.Resolved).modifier
        val heightMod = (heightRes as LayoutResolver.LayoutModifierResult.Resolved).modifier
        val marginMod = LayoutResolver.resolveMargin(widthMod.then(heightMod), template.margin)
        val paddingMod = LayoutResolver.resolvePadding(marginMod, template.padding)

        val verticalAlignment = LayoutResolver.resolveColumnAlignment(template.alignment)
        val verticalArrangement = (arrangementRes as LayoutResolver.LayoutArrangementResult.Vertical.Resolved).arrangement

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
