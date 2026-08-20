package com.carbroz.partner.sdui.render.renderer.child

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.runtime.Composable
import com.carbroz.partner.core.ui.adaptive.context.CurrentContainerConstraints
import com.carbroz.partner.core.ui.adaptive.context.ResolutionAxis
import com.carbroz.partner.core.ui.adaptive.context.ResolutionContext
import com.carbroz.partner.core.ui.adaptive.result.ResolutionResult
import com.carbroz.partner.sdui.engine.model.LayoutAxis
import com.carbroz.partner.sdui.engine.model.SduiChild
import com.carbroz.partner.sdui.render.fallback.UnsupportedFallback
import com.carbroz.partner.sdui.render.resolver.LayoutResolver
import com.carbroz.partner.sdui.render.scope.RenderScope

public object ContainerChild : ChildRenderer {
    @Composable
    override fun render(child: SduiChild, scope: RenderScope) {
        if (!child.visible) return

        val widthRes = LayoutResolver.resolveWidth(child.width, scope.resolutionContext)
        val heightRes = LayoutResolver.resolveHeight(child.height, scope.resolutionContext)

        if (widthRes !is ResolutionResult.Resolved || heightRes !is ResolutionResult.Resolved) {
            UnsupportedFallback.renderUnsupportedChild(child, scope)
            return
        }

        val marginMod = LayoutResolver.resolveMargin(widthRes.value.then(heightRes.value), child.margin)
        val paddingMod = LayoutResolver.resolvePadding(marginMod, child.padding)

        BoxWithConstraints(modifier = paddingMod) {
            val localContext = ResolutionContext(
                axis = ResolutionAxis.HORIZONTAL,
                container = CurrentContainerConstraints(availableWidth = maxWidth, availableHeight = maxHeight)
            )
            val childScope = scope.withResolutionContext(localContext)

            if (child.axis == LayoutAxis.HORIZONTAL) {
                val arrangementRes = LayoutResolver.resolveRowArrangement(child.arrangement, child.gap, localContext)
                if (arrangementRes !is ResolutionResult.Resolved) {
                    UnsupportedFallback.renderUnsupportedChild(child, scope)
                    return@BoxWithConstraints
                }
                val horizontalAlignment = LayoutResolver.resolveRowAlignment(child.alignment)
                Row(
                    horizontalArrangement = arrangementRes.value,
                    verticalAlignment = horizontalAlignment
                ) {
                    for (cd in child.childrenData) {
                        childScope.renderChildrenData(cd)
                    }
                }
            } else {
                val arrangementRes = LayoutResolver.resolveColumnArrangement(child.arrangement, child.gap, localContext)
                if (arrangementRes !is ResolutionResult.Resolved) {
                    UnsupportedFallback.renderUnsupportedChild(child, scope)
                    return@BoxWithConstraints
                }
                val verticalAlignment = LayoutResolver.resolveColumnAlignment(child.alignment)
                Column(
                    verticalArrangement = arrangementRes.value,
                    horizontalAlignment = verticalAlignment
                ) {
                    for (cd in child.childrenData) {
                        childScope.renderChildrenData(cd)
                    }
                }
            }
        }
    }
}
