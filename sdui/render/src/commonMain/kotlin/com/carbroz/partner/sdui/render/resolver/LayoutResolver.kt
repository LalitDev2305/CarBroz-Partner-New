package com.carbroz.partner.sdui.render.resolver

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.carbroz.partner.core.ui.adaptive.context.CurrentContainerConstraints
import com.carbroz.partner.core.ui.adaptive.context.ResolutionAxis
import com.carbroz.partner.core.ui.adaptive.context.ResolutionContext
import com.carbroz.partner.core.ui.adaptive.resolver.DimensionResolver
import com.carbroz.partner.core.ui.adaptive.resolver.SpacingResolver
import com.carbroz.partner.core.ui.adaptive.result.ResolutionResult
import com.carbroz.partner.core.ui.adaptive.result.ResolvedDimension
import com.carbroz.partner.core.ui.adaptive.spec.DimensionSpec
import com.carbroz.partner.core.ui.adaptive.spec.SpacingSpec
import com.carbroz.partner.core.ui.tokens.DimensionTokenResolver
import com.carbroz.partner.core.ui.tokens.SpacingTokenResolver
import com.carbroz.partner.sdui.engine.model.LayoutAlignment
import com.carbroz.partner.sdui.engine.model.LayoutArrangement
import com.carbroz.partner.sdui.engine.model.SduiEdgeSpacing

public object LayoutResolver {

    public fun resolveWidth(
        spec: DimensionSpec,
        context: ResolutionContext? = null,
        tokenResolver: DimensionTokenResolver? = null
    ): Modifier {
        val hContext = context ?: ResolutionContext(
            axis = ResolutionAxis.HORIZONTAL,
            container = CurrentContainerConstraints(0.dp, 0.dp)
        )
        return when (val result = DimensionResolver.resolve(spec, hContext, tokenResolver = tokenResolver)) {
            is ResolutionResult.Resolved -> when (val dim = result.value) {
                is ResolvedDimension.Exact -> Modifier.width(dim.valueDp)
                is ResolvedDimension.Fill -> Modifier.fillMaxWidth()
                is ResolvedDimension.Wrap -> Modifier
            }
            is ResolutionResult.Unsupported -> Modifier
        }
    }

    public fun resolveHeight(
        spec: DimensionSpec,
        context: ResolutionContext? = null,
        tokenResolver: DimensionTokenResolver? = null
    ): Modifier {
        val vContext = context ?: ResolutionContext(
            axis = ResolutionAxis.VERTICAL,
            container = CurrentContainerConstraints(0.dp, 0.dp)
        )
        return when (val result = DimensionResolver.resolve(spec, vContext, tokenResolver = tokenResolver)) {
            is ResolutionResult.Resolved -> when (val dim = result.value) {
                is ResolvedDimension.Exact -> Modifier.height(dim.valueDp)
                is ResolvedDimension.Fill -> Modifier.fillMaxHeight()
                is ResolvedDimension.Wrap -> Modifier
            }
            is ResolutionResult.Unsupported -> Modifier
        }
    }

    public fun resolveEdgeSpacing(edge: SduiEdgeSpacing): PaddingValues = PaddingValues(
        start = edge.start.dp,
        top = edge.top.dp,
        end = edge.end.dp,
        bottom = edge.bottom.dp
    )

    public fun resolvePadding(modifier: Modifier, padding: SduiEdgeSpacing): Modifier =
        modifier.padding(resolveEdgeSpacing(padding))

    public fun resolveMargin(modifier: Modifier, margin: SduiEdgeSpacing): Modifier =
        modifier.padding(resolveEdgeSpacing(margin))

    public fun resolveGap(
        spec: SpacingSpec,
        context: ResolutionContext? = null,
        tokenResolver: SpacingTokenResolver? = null
    ): DpSpec {
        val axisContext = context ?: ResolutionContext(
            axis = ResolutionAxis.HORIZONTAL,
            container = CurrentContainerConstraints(0.dp, 0.dp)
        )
        return when (val result = SpacingResolver.resolve(spec, axisContext, tokenResolver = tokenResolver)) {
            is ResolutionResult.Resolved -> DpSpec(result.value)
            is ResolutionResult.Unsupported -> DpSpec(0.dp)
        }
    }

    public fun resolveColumnAlignment(alignment: LayoutAlignment): Alignment.Horizontal = when (alignment) {
        LayoutAlignment.CENTER -> Alignment.CenterHorizontally
        LayoutAlignment.END -> Alignment.End
        else -> Alignment.Start
    }

    public fun resolveRowAlignment(alignment: LayoutAlignment): Alignment.Vertical = when (alignment) {
        LayoutAlignment.CENTER -> Alignment.CenterVertically
        LayoutAlignment.END -> Alignment.Bottom
        else -> Alignment.Top
    }

    public fun resolveColumnArrangement(
        arrangement: LayoutArrangement,
        gap: SpacingSpec,
        context: ResolutionContext? = null,
        tokenResolver: SpacingTokenResolver? = null
    ): Arrangement.Vertical {
        val spacing = resolveGap(gap, context, tokenResolver).value
        return when (arrangement) {
            LayoutArrangement.CENTER -> Arrangement.Center
            LayoutArrangement.END -> Arrangement.Bottom
            LayoutArrangement.SPACE_BETWEEN -> Arrangement.SpaceBetween
            LayoutArrangement.SPACE_AROUND -> Arrangement.SpaceAround
            LayoutArrangement.SPACE_EVENLY -> Arrangement.SpaceEvenly
            else -> Arrangement.spacedBy(spacing)
        }
    }

    public fun resolveRowArrangement(
        arrangement: LayoutArrangement,
        gap: SpacingSpec,
        context: ResolutionContext? = null,
        tokenResolver: SpacingTokenResolver? = null
    ): Arrangement.Horizontal {
        val spacing = resolveGap(gap, context, tokenResolver).value
        return when (arrangement) {
            LayoutArrangement.CENTER -> Arrangement.Center
            LayoutArrangement.END -> Arrangement.End
            LayoutArrangement.SPACE_BETWEEN -> Arrangement.SpaceBetween
            LayoutArrangement.SPACE_AROUND -> Arrangement.SpaceAround
            LayoutArrangement.SPACE_EVENLY -> Arrangement.SpaceEvenly
            else -> Arrangement.spacedBy(spacing)
        }
    }

    public data class DpSpec(val value: Dp)
}
