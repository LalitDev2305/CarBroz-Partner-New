package com.carbroz.partner.core.ui.adaptive.resolver

import com.carbroz.partner.core.ui.adaptive.context.ResolutionAxis
import com.carbroz.partner.core.ui.adaptive.context.ResolutionContext
import com.carbroz.partner.core.ui.adaptive.result.ResolutionResult
import com.carbroz.partner.core.ui.adaptive.result.ResolvedDimension
import com.carbroz.partner.core.ui.adaptive.scaling.AdaptiveScaleCalculator
import com.carbroz.partner.core.ui.adaptive.scaling.AdaptiveScalePolicy
import com.carbroz.partner.core.ui.adaptive.scaling.DesignReferenceSpace
import com.carbroz.partner.core.ui.adaptive.spec.DimensionSpec
import com.carbroz.partner.core.ui.adaptive.spec.LayoutConstraintSpec
import com.carbroz.partner.core.ui.tokens.DimensionTokenResolver
import androidx.compose.ui.unit.dp

object DimensionResolver {

    fun resolve(
        spec: DimensionSpec,
        context: ResolutionContext,
        constraint: LayoutConstraintSpec? = null,
        policy: AdaptiveScalePolicy? = null,
        tokenResolver: DimensionTokenResolver? = null
    ): ResolutionResult<ResolvedDimension> {
        val rawResolved: ResolutionResult<ResolvedDimension> = when (spec) {
            is DimensionSpec.Fixed -> ResolutionResult.Resolved(ResolvedDimension.Exact(spec.valueDp))

            is DimensionSpec.Adaptive -> {
                val available = if (context.axis == ResolutionAxis.HORIZONTAL) {
                    context.container.availableWidth
                } else {
                    context.container.availableHeight
                }

                val activePolicy = policy ?: AdaptiveScalePolicy(
                    referenceDp = if (context.axis == ResolutionAxis.HORIZONTAL) {
                        DesignReferenceSpace.baselineWidthDp
                    } else {
                        DesignReferenceSpace.baselineHeightDp
                    }
                )

                val scaled = AdaptiveScaleCalculator.calculate(
                    availableDimension = available,
                    baseValue = spec.baseDp,
                    policy = activePolicy,
                    minDp = spec.minDp,
                    maxDp = spec.maxDp
                )

                ResolutionResult.Resolved(ResolvedDimension.Exact(scaled))
            }

            is DimensionSpec.Fraction -> {
                val avail = if (context.axis == ResolutionAxis.HORIZONTAL) {
                    context.container.availableWidth
                } else {
                    context.container.availableHeight
                }
                val valDp = (avail.value * spec.percentage).dp
                ResolutionResult.Resolved(ResolvedDimension.Exact(valDp))
            }

            is DimensionSpec.Fill -> ResolutionResult.Resolved(ResolvedDimension.Fill)

            is DimensionSpec.Wrap -> ResolutionResult.Resolved(ResolvedDimension.Wrap)

            is DimensionSpec.Token -> {
                val tokenVal = tokenResolver?.resolveDimension(spec.key)
                if (tokenVal != null) {
                    ResolutionResult.Resolved(ResolvedDimension.Exact(tokenVal))
                } else {
                    ResolutionResult.Unsupported(spec.key)
                }
            }
        }

        return applyConstraints(rawResolved, constraint)
    }

    private fun applyConstraints(
        result: ResolutionResult<ResolvedDimension>,
        constraint: LayoutConstraintSpec?
    ): ResolutionResult<ResolvedDimension> {
        if (constraint == null || result !is ResolutionResult.Resolved) return result
        val value = result.value
        if (value !is ResolvedDimension.Exact) return result

        var currentDp = value.valueDp
        if (constraint.minDp != null && currentDp < constraint.minDp) {
            currentDp = constraint.minDp
        }
        if (constraint.maxDp != null && currentDp > constraint.maxDp) {
            currentDp = constraint.maxDp
        }

        return ResolutionResult.Resolved(ResolvedDimension.Exact(currentDp))
    }
}
