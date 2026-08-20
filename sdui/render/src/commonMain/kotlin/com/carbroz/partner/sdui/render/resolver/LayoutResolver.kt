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

    public sealed interface LayoutModifierResult {
        public data class Resolved(val modifier: Modifier) : LayoutModifierResult
        public data class UnsupportedToken(val reason: String) : LayoutModifierResult
    }

    public sealed interface LayoutSpacingResult {
        public data class Resolved(val dpSpec: DpSpec) : LayoutSpacingResult
        public data class UnsupportedToken(val reason: String) : LayoutSpacingResult
    }

    public sealed interface LayoutArrangementResult {
        public sealed interface Vertical {
            public data class Resolved(val arrangement: Arrangement.Vertical) : Vertical
            public data class UnsupportedToken(val reason: String) : Vertical
        }
        public sealed interface Horizontal {
            public data class Resolved(val arrangement: Arrangement.Horizontal) : Horizontal
            public data class UnsupportedToken(val reason: String) : Horizontal
        }
    }

    public fun resolveWidth(
        spec: DimensionSpec,
        context: ResolutionContext,
        tokenResolver: DimensionTokenResolver? = null
    ): LayoutModifierResult {
        val hContext = if (context.axis == ResolutionAxis.HORIZONTAL) context else context.copy(axis = ResolutionAxis.HORIZONTAL)
        return when (val result = DimensionResolver.resolve(spec, hContext, tokenResolver = tokenResolver)) {
            is ResolutionResult.Resolved -> when (val dim = result.value) {
                is ResolvedDimension.Exact -> LayoutModifierResult.Resolved(Modifier.width(dim.valueDp))
                is ResolvedDimension.Fill -> LayoutModifierResult.Resolved(Modifier.fillMaxWidth())
                is ResolvedDimension.Wrap -> LayoutModifierResult.Resolved(Modifier)
            }
            is ResolutionResult.Unsupported -> LayoutModifierResult.UnsupportedToken("Layout dimension token could not be resolved")
        }
    }

    public fun resolveHeight(
        spec: DimensionSpec,
        context: ResolutionContext,
        tokenResolver: DimensionTokenResolver? = null
    ): LayoutModifierResult {
        val vContext = if (context.axis == ResolutionAxis.VERTICAL) context else context.copy(axis = ResolutionAxis.VERTICAL)
        return when (val result = DimensionResolver.resolve(spec, vContext, tokenResolver = tokenResolver)) {
            is ResolutionResult.Resolved -> when (val dim = result.value) {
                is ResolvedDimension.Exact -> LayoutModifierResult.Resolved(Modifier.height(dim.valueDp))
                is ResolvedDimension.Fill -> LayoutModifierResult.Resolved(Modifier.fillMaxHeight())
                is ResolvedDimension.Wrap -> LayoutModifierResult.Resolved(Modifier)
            }
            is ResolutionResult.Unsupported -> LayoutModifierResult.UnsupportedToken("Layout dimension token could not be resolved")
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
    ): LayoutSpacingResult {
        return when (val result = SpacingResolver.resolve(spec, context, tokenResolver = tokenResolver)) {
            is ResolutionResult.Resolved -> LayoutSpacingResult.Resolved(DpSpec(result.value))
            is ResolutionResult.Unsupported -> LayoutSpacingResult.UnsupportedToken("Layout spacing token could not be resolved")
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
        context: ResolutionContext,
        tokenResolver: SpacingTokenResolver? = null
    ): LayoutArrangementResult.Vertical {
        val vContext = if (context.axis == ResolutionAxis.VERTICAL) context else context.copy(axis = ResolutionAxis.VERTICAL)
        return when (val gapRes = resolveGap(gap, vContext, tokenResolver)) {
            is LayoutSpacingResult.Resolved -> {
                val spacing = gapRes.dpSpec.value
                val arr = when (arrangement) {
                    LayoutArrangement.CENTER -> Arrangement.Center
                    LayoutArrangement.END -> Arrangement.Bottom
                    LayoutArrangement.SPACE_BETWEEN -> Arrangement.SpaceBetween
                    LayoutArrangement.SPACE_AROUND -> Arrangement.SpaceAround
                    LayoutArrangement.SPACE_EVENLY -> Arrangement.SpaceEvenly
                    else -> Arrangement.spacedBy(spacing)
                }
                LayoutArrangementResult.Vertical.Resolved(arr)
            }
            is LayoutSpacingResult.UnsupportedToken -> LayoutArrangementResult.Vertical.UnsupportedToken(gapRes.reason)
        }
    }

    public fun resolveRowArrangement(
        arrangement: LayoutArrangement,
        gap: SpacingSpec,
        context: ResolutionContext,
        tokenResolver: SpacingTokenResolver? = null
    ): LayoutArrangementResult.Horizontal {
        val hContext = if (context.axis == ResolutionAxis.HORIZONTAL) context else context.copy(axis = ResolutionAxis.HORIZONTAL)
        return when (val gapRes = resolveGap(gap, hContext, tokenResolver)) {
            is LayoutSpacingResult.Resolved -> {
                val spacing = gapRes.dpSpec.value
                val arr = when (arrangement) {
                    LayoutArrangement.CENTER -> Arrangement.Center
                    LayoutArrangement.END -> Arrangement.End
                    LayoutArrangement.SPACE_BETWEEN -> Arrangement.SpaceBetween
                    LayoutArrangement.SPACE_AROUND -> Arrangement.SpaceAround
                    LayoutArrangement.SPACE_EVENLY -> Arrangement.SpaceEvenly
                    else -> Arrangement.spacedBy(spacing)
                }
                LayoutArrangementResult.Horizontal.Resolved(arr)
            }
            is LayoutSpacingResult.UnsupportedToken -> LayoutArrangementResult.Horizontal.UnsupportedToken(gapRes.reason)
        }
    }

    public data class DpSpec(val value: Dp)
}
