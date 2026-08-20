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
        context: ResolutionContext,
        tokenResolver: DimensionTokenResolver? = null
    ): ResolutionResult<Modifier> {
        val hContext = if (context.axis == ResolutionAxis.HORIZONTAL) context else context.copy(axis = ResolutionAxis.HORIZONTAL)
        return when (val result = DimensionResolver.resolve(spec, hContext, tokenResolver = tokenResolver)) {
            is ResolutionResult.Resolved -> {
                val modifier = when (val dim = result.value) {
                    is ResolvedDimension.Exact -> Modifier.width(dim.valueDp)
                    is ResolvedDimension.Fill -> Modifier.fillMaxWidth()
                    is ResolvedDimension.Wrap -> Modifier
                }
                ResolutionResult.Resolved(modifier)
            }
            is ResolutionResult.Unsupported -> ResolutionResult.Unsupported(result.tokenKey)
        }
    }

    public fun resolveHeight(
        spec: DimensionSpec,
        context: ResolutionContext,
        tokenResolver: DimensionTokenResolver? = null
    ): ResolutionResult<Modifier> {
        val vContext = if (context.axis == ResolutionAxis.VERTICAL) context else context.copy(axis = ResolutionAxis.VERTICAL)
        return when (val result = DimensionResolver.resolve(spec, vContext, tokenResolver = tokenResolver)) {
            is ResolutionResult.Resolved -> {
                val modifier = when (val dim = result.value) {
                    is ResolvedDimension.Exact -> Modifier.height(dim.valueDp)
                    is ResolvedDimension.Fill -> Modifier.fillMaxHeight()
                    is ResolvedDimension.Wrap -> Modifier
                }
                ResolutionResult.Resolved(modifier)
            }
            is ResolutionResult.Unsupported -> ResolutionResult.Unsupported(result.tokenKey)
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
        context: ResolutionContext,
        tokenResolver: SpacingTokenResolver? = null
    ): ResolutionResult<Dp> {
        return SpacingResolver.resolve(spec, context, tokenResolver = tokenResolver)
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
        context: ResolutionContext,
        tokenResolver: SpacingTokenResolver? = null
    ): ResolutionResult<Arrangement.Vertical> {
        val vContext = if (context.axis == ResolutionAxis.VERTICAL) context else context.copy(axis = ResolutionAxis.VERTICAL)
        return when (val gapRes = resolveGap(gap, vContext, tokenResolver)) {
            is ResolutionResult.Resolved -> {
                val spacing = gapRes.value
                val arr = when (arrangement) {
                    LayoutArrangement.CENTER -> Arrangement.Center
                    LayoutArrangement.END -> Arrangement.Bottom
                    LayoutArrangement.SPACE_BETWEEN -> Arrangement.SpaceBetween
                    LayoutArrangement.SPACE_AROUND -> Arrangement.SpaceAround
                    LayoutArrangement.SPACE_EVENLY -> Arrangement.SpaceEvenly
                    else -> Arrangement.spacedBy(spacing)
                }
                ResolutionResult.Resolved(arr)
            }
            is ResolutionResult.Unsupported -> ResolutionResult.Unsupported(gapRes.tokenKey)
        }
    }

    public fun resolveRowArrangement(
        arrangement: LayoutArrangement,
        gap: SpacingSpec,
        context: ResolutionContext,
        tokenResolver: SpacingTokenResolver? = null
    ): ResolutionResult<Arrangement.Horizontal> {
        val hContext = if (context.axis == ResolutionAxis.HORIZONTAL) context else context.copy(axis = ResolutionAxis.HORIZONTAL)
        return when (val gapRes = resolveGap(gap, hContext, tokenResolver)) {
            is ResolutionResult.Resolved -> {
                val spacing = gapRes.value
                val arr = when (arrangement) {
                    LayoutArrangement.CENTER -> Arrangement.Center
                    LayoutArrangement.END -> Arrangement.End
                    LayoutArrangement.SPACE_BETWEEN -> Arrangement.SpaceBetween
                    LayoutArrangement.SPACE_AROUND -> Arrangement.SpaceAround
                    LayoutArrangement.SPACE_EVENLY -> Arrangement.SpaceEvenly
                    else -> Arrangement.spacedBy(spacing)
                }
                ResolutionResult.Resolved(arr)
            }
            is ResolutionResult.Unsupported -> ResolutionResult.Unsupported(gapRes.tokenKey)
        }
    }
}
