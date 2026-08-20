package com.carbroz.partner.sdui.render.renderer.template

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.runtime.Composable
import com.carbroz.partner.sdui.engine.model.LayoutAxis
import com.carbroz.partner.sdui.engine.model.SduiTemplate
import com.carbroz.partner.sdui.render.fallback.UnsupportedFallback
import com.carbroz.partner.sdui.render.resolver.LayoutResolver
import com.carbroz.partner.sdui.render.scope.RenderScope

public object ContainerTemplate : TemplateRenderer {
    @Composable
    override fun render(template: SduiTemplate, scope: RenderScope) {
        val widthRes = LayoutResolver.resolveWidth(template.width, scope.resolutionContext)
        val heightRes = LayoutResolver.resolveHeight(template.height, scope.resolutionContext)

        if (widthRes is LayoutResolver.LayoutModifierResult.UnsupportedToken ||
            heightRes is LayoutResolver.LayoutModifierResult.UnsupportedToken
        ) {
            UnsupportedFallback.renderUnsupportedTemplate(template, scope)
            return
        }

        val widthMod = (widthRes as LayoutResolver.LayoutModifierResult.Resolved).modifier
        val heightMod = (heightRes as LayoutResolver.LayoutModifierResult.Resolved).modifier
        val marginMod = LayoutResolver.resolveMargin(widthMod.then(heightMod), template.margin)
        val paddingMod = LayoutResolver.resolvePadding(marginMod, template.padding)

        if (template.axis == LayoutAxis.HORIZONTAL) {
            val arrangementRes = LayoutResolver.resolveRowArrangement(template.arrangement, template.gap, scope.resolutionContext)
            if (arrangementRes is LayoutResolver.LayoutArrangementResult.Horizontal.UnsupportedToken) {
                UnsupportedFallback.renderUnsupportedTemplate(template, scope)
                return
            }
            val horizontalAlignment = LayoutResolver.resolveRowAlignment(template.alignment)
            val horizontalArrangement = (arrangementRes as LayoutResolver.LayoutArrangementResult.Horizontal.Resolved).arrangement
            Row(
                modifier = paddingMod,
                horizontalArrangement = horizontalArrangement,
                verticalAlignment = horizontalAlignment
            ) {
                for (comp in template.components) {
                    scope.renderComponent(comp)
                }
            }
        } else {
            val arrangementRes = LayoutResolver.resolveColumnArrangement(template.arrangement, template.gap, scope.resolutionContext)
            if (arrangementRes is LayoutResolver.LayoutArrangementResult.Vertical.UnsupportedToken) {
                UnsupportedFallback.renderUnsupportedTemplate(template, scope)
                return
            }
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
}
