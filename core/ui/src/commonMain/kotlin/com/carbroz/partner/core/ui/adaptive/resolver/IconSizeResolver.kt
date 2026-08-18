package com.carbroz.partner.core.ui.adaptive.resolver

import com.carbroz.partner.core.ui.adaptive.context.ResolutionAxis
import com.carbroz.partner.core.ui.adaptive.context.ResolutionContext
import com.carbroz.partner.core.ui.adaptive.result.ResolutionResult
import com.carbroz.partner.core.ui.adaptive.scaling.AdaptiveScaleCalculator
import com.carbroz.partner.core.ui.adaptive.scaling.AdaptiveScalePolicy
import com.carbroz.partner.core.ui.adaptive.scaling.DesignReferenceSpace
import com.carbroz.partner.core.ui.adaptive.spec.IconSizeSpec
import com.carbroz.partner.core.ui.tokens.IconTokenResolver
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

object IconSizeResolver {

    fun resolve(
        spec: IconSizeSpec,
        context: ResolutionContext,
        tokenResolver: IconTokenResolver? = null
    ): ResolutionResult<Dp> {
        return when (spec) {
            is IconSizeSpec.Fixed -> ResolutionResult.Resolved(spec.sizeDp)
            is IconSizeSpec.Adaptive -> {
                val available = if (context.axis == ResolutionAxis.HORIZONTAL) context.container.availableWidth else context.container.availableHeight
                val policy = AdaptiveScalePolicy(
                    referenceDp = if (context.axis == ResolutionAxis.HORIZONTAL) DesignReferenceSpace.baselineWidthDp else DesignReferenceSpace.baselineHeightDp
                )
                val scaled = AdaptiveScaleCalculator.calculate(available, spec.baseDp, policy)
                ResolutionResult.Resolved(scaled)
            }
            is IconSizeSpec.Token -> {
                val resolved = tokenResolver?.resolveIconSize(spec.key)
                if (resolved != null) {
                    ResolutionResult.Resolved(resolved)
                } else {
                    ResolutionResult.Unsupported(spec.key)
                }
            }
        }
    }
}
