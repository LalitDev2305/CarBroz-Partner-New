package com.carbroz.partner.sdui.render.renderer.subcomponent

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.runtime.Composable
import com.carbroz.partner.core.ui.adaptive.context.CurrentContainerConstraints
import com.carbroz.partner.core.ui.adaptive.context.ResolutionAxis
import com.carbroz.partner.core.ui.adaptive.context.ResolutionContext
import com.carbroz.partner.core.ui.adaptive.result.ResolutionResult
import com.carbroz.partner.sdui.engine.model.LayoutAxis
import com.carbroz.partner.sdui.engine.model.SduiSubComponent
import com.carbroz.partner.sdui.render.fallback.UnsupportedFallback
import com.carbroz.partner.sdui.render.resolver.LayoutResolver
import com.carbroz.partner.sdui.render.scope.RenderScope

public object ContainerSubComponent : SubComponentRenderer {
    @Composable
    override fun render(subComponent: SduiSubComponent, scope: RenderScope) {
        if (!subComponent.visible) return

        val widthRes = LayoutResolver.resolveWidth(subComponent.width, scope.resolutionContext)
        val heightRes = LayoutResolver.resolveHeight(subComponent.height, scope.resolutionContext)

        if (widthRes !is ResolutionResult.Resolved || heightRes !is ResolutionResult.Resolved) {
            UnsupportedFallback.renderUnsupportedSubComponent(subComponent, scope)
            return
        }

        val marginMod = LayoutResolver.resolveMargin(widthRes.value.then(heightRes.value), subComponent.margin)
        val paddingMod = LayoutResolver.resolvePadding(marginMod, subComponent.padding)

        BoxWithConstraints(modifier = paddingMod) {
            val childContext = ResolutionContext(
                axis = ResolutionAxis.HORIZONTAL,
                container = CurrentContainerConstraints(availableWidth = maxWidth, availableHeight = maxHeight)
            )
            val childScope = scope.withResolutionContext(childContext)

            if (subComponent.axis == LayoutAxis.HORIZONTAL) {
                val arrangementRes = LayoutResolver.resolveRowArrangement(subComponent.arrangement, subComponent.gap, scope.resolutionContext)
                if (arrangementRes !is ResolutionResult.Resolved) {
                    UnsupportedFallback.renderUnsupportedSubComponent(subComponent, scope)
                    return@BoxWithConstraints
                }
                val horizontalAlignment = LayoutResolver.resolveRowAlignment(subComponent.alignment)
                Row(
                    horizontalArrangement = arrangementRes.value,
                    verticalAlignment = horizontalAlignment
                ) {
                    for (ch in subComponent.children) {
                        childScope.renderChild(ch)
                    }
                    for (cd in subComponent.childrenData) {
                        childScope.renderChildrenData(cd)
                    }
                }
            } else {
                val arrangementRes = LayoutResolver.resolveColumnArrangement(subComponent.arrangement, subComponent.gap, scope.resolutionContext)
                if (arrangementRes !is ResolutionResult.Resolved) {
                    UnsupportedFallback.renderUnsupportedSubComponent(subComponent, scope)
                    return@BoxWithConstraints
                }
                val verticalAlignment = LayoutResolver.resolveColumnAlignment(subComponent.alignment)
                Column(
                    verticalArrangement = arrangementRes.value,
                    horizontalAlignment = verticalAlignment
                ) {
                    for (ch in subComponent.children) {
                        childScope.renderChild(ch)
                    }
                    for (cd in subComponent.childrenData) {
                        childScope.renderChildrenData(cd)
                    }
                }
            }
        }
    }
}
