package com.carbroz.partner.sdui.render.renderer.component

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.runtime.Composable
import com.carbroz.partner.core.ui.adaptive.context.CurrentContainerConstraints
import com.carbroz.partner.core.ui.adaptive.context.ResolutionAxis
import com.carbroz.partner.core.ui.adaptive.context.ResolutionContext
import com.carbroz.partner.core.ui.adaptive.result.ResolutionResult
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

        if (widthRes !is ResolutionResult.Resolved || heightRes !is ResolutionResult.Resolved) {
            UnsupportedFallback.renderUnsupportedComponent(component, scope)
            return
        }

        val marginMod = LayoutResolver.resolveMargin(widthRes.value.then(heightRes.value), component.margin)
        val paddingMod = LayoutResolver.resolvePadding(marginMod, component.padding)

        BoxWithConstraints(modifier = paddingMod) {
            val childContext = ResolutionContext(
                axis = ResolutionAxis.HORIZONTAL,
                container = CurrentContainerConstraints(availableWidth = maxWidth, availableHeight = maxHeight)
            )
            val childScope = scope.withResolutionContext(childContext)

            if (component.axis == LayoutAxis.HORIZONTAL) {
                val arrangementRes = LayoutResolver.resolveRowArrangement(component.arrangement, component.gap, scope.resolutionContext)
                if (arrangementRes !is ResolutionResult.Resolved) {
                    UnsupportedFallback.renderUnsupportedComponent(component, scope)
                    return@BoxWithConstraints
                }
                val horizontalAlignment = LayoutResolver.resolveRowAlignment(component.alignment)
                Row(
                    horizontalArrangement = arrangementRes.value,
                    verticalAlignment = horizontalAlignment
                ) {
                    for (sub in component.subcomponents) {
                        childScope.renderSubComponent(sub)
                    }
                    for (cd in component.childrenData) {
                        childScope.renderChildrenData(cd)
                    }
                }
            } else {
                val arrangementRes = LayoutResolver.resolveColumnArrangement(component.arrangement, component.gap, scope.resolutionContext)
                if (arrangementRes !is ResolutionResult.Resolved) {
                    UnsupportedFallback.renderUnsupportedComponent(component, scope)
                    return@BoxWithConstraints
                }
                val verticalAlignment = LayoutResolver.resolveColumnAlignment(component.alignment)
                Column(
                    verticalArrangement = arrangementRes.value,
                    horizontalAlignment = verticalAlignment
                ) {
                    for (sub in component.subcomponents) {
                        childScope.renderSubComponent(sub)
                    }
                    for (cd in component.childrenData) {
                        childScope.renderChildrenData(cd)
                    }
                }
            }
        }
    }
}
