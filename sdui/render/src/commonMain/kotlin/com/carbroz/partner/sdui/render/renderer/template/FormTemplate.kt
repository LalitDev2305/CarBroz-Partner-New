package com.carbroz.partner.sdui.render.renderer.template

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import com.carbroz.partner.core.ui.adaptive.context.CurrentContainerConstraints
import com.carbroz.partner.core.ui.adaptive.context.ResolutionAxis
import com.carbroz.partner.core.ui.adaptive.context.ResolutionContext
import com.carbroz.partner.core.ui.adaptive.result.ResolutionResult
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

        if (widthRes !is ResolutionResult.Resolved ||
            heightRes !is ResolutionResult.Resolved ||
            arrangementRes !is ResolutionResult.Resolved
        ) {
            UnsupportedFallback.renderUnsupportedTemplate(template, scope)
            return
        }

        val marginMod = LayoutResolver.resolveMargin(widthRes.value.then(heightRes.value), template.margin)
        val paddingMod = LayoutResolver.resolvePadding(marginMod, template.padding)
        val verticalAlignment = LayoutResolver.resolveColumnAlignment(template.alignment)

        BoxWithConstraints(modifier = paddingMod) {
            val childContext = ResolutionContext(
                axis = ResolutionAxis.HORIZONTAL,
                container = CurrentContainerConstraints(availableWidth = maxWidth, availableHeight = maxHeight)
            )
            val childScope = scope.withResolutionContext(childContext)

            Column(
                verticalArrangement = arrangementRes.value,
                horizontalAlignment = verticalAlignment
            ) {
                for (comp in template.components) {
                    childScope.renderComponent(comp)
                }
            }
        }
    }
}
