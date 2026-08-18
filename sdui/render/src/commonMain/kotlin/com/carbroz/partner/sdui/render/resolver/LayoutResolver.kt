package com.carbroz.partner.sdui.render.resolver

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.carbroz.partner.core.ui.adaptive.spec.DimensionSpec
import com.carbroz.partner.core.ui.adaptive.spec.SpacingSpec
import com.carbroz.partner.sdui.engine.model.LayoutAlignment
import com.carbroz.partner.sdui.engine.model.LayoutArrangement
import com.carbroz.partner.sdui.engine.model.SduiEdgeSpacing

public object LayoutResolver {

    public fun resolveWidth(spec: DimensionSpec): Modifier = when (spec) {
        is DimensionSpec.Fill -> Modifier.fillMaxWidth()
        is DimensionSpec.Fixed -> Modifier.width(spec.valueDp)
        is DimensionSpec.Fraction -> Modifier.fillMaxWidth(spec.percentage)
        is DimensionSpec.Wrap -> Modifier
        is DimensionSpec.Adaptive -> Modifier.fillMaxWidth()
        is DimensionSpec.Token -> Modifier.fillMaxWidth()
    }

    public fun resolveHeight(spec: DimensionSpec): Modifier = when (spec) {
        is DimensionSpec.Fill -> Modifier.fillMaxHeight()
        is DimensionSpec.Fixed -> Modifier.height(spec.valueDp)
        is DimensionSpec.Fraction -> Modifier.fillMaxHeight(spec.percentage)
        is DimensionSpec.Wrap -> Modifier
        is DimensionSpec.Adaptive -> Modifier.fillMaxHeight()
        is DimensionSpec.Token -> Modifier.fillMaxHeight()
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

    public fun resolveGap(spec: SpacingSpec): DpSpec = when (spec) {
        is SpacingSpec.Fixed -> DpSpec(spec.spaceDp)
        is SpacingSpec.Adaptive -> DpSpec(spec.baseDp)
        is SpacingSpec.Token -> DpSpec(16.dp)
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

    public fun resolveColumnArrangement(arrangement: LayoutArrangement, gap: SpacingSpec): Arrangement.Vertical {
        val spacing = resolveGap(gap).value
        return when (arrangement) {
            LayoutArrangement.CENTER -> Arrangement.Center
            LayoutArrangement.END -> Arrangement.Bottom
            LayoutArrangement.SPACE_BETWEEN -> Arrangement.SpaceBetween
            LayoutArrangement.SPACE_AROUND -> Arrangement.SpaceAround
            LayoutArrangement.SPACE_EVENLY -> Arrangement.SpaceEvenly
            else -> Arrangement.spacedBy(spacing)
        }
    }

    public fun resolveRowArrangement(arrangement: LayoutArrangement, gap: SpacingSpec): Arrangement.Horizontal {
        val spacing = resolveGap(gap).value
        return when (arrangement) {
            LayoutArrangement.CENTER -> Arrangement.Center
            LayoutArrangement.END -> Arrangement.End
            LayoutArrangement.SPACE_BETWEEN -> Arrangement.SpaceBetween
            LayoutArrangement.SPACE_AROUND -> Arrangement.SpaceAround
            LayoutArrangement.SPACE_EVENLY -> Arrangement.SpaceEvenly
            else -> Arrangement.spacedBy(spacing)
        }
    }

    public data class DpSpec(val value: androidx.compose.ui.unit.Dp)
}
