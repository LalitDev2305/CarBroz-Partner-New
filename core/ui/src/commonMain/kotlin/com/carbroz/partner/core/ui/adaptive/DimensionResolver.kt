package com.carbroz.partner.core.ui.adaptive

import com.carbroz.partner.core.ui.tokens.DimensionTokenResolver
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Single canonical resolver for container dimension specifications.
 *
 * Preserves explicit ResolvedDimension semantics (Exact vs Fill vs Wrap) to prevent layout intent loss.
 */
object DimensionResolver {

    private val dimensionPolicy = AdaptiveScalePolicy(
        referenceDp = DesignReferenceSpace.widthDp,
        dampingExponent = 0.5f,
        minScaleFactor = 0.8f,
        maxScaleFactor = 2.0f
    )

    fun resolve(
        spec: DimensionSpec,
        context: ResolutionContext,
        constraint: LayoutConstraintSpec? = null,
        tokenResolver: DimensionTokenResolver? = null
    ): ResolutionResult<ResolvedDimension> {
        val available = context.availableDimension

        if (constraint != null && constraint.minDp != null && constraint.maxDp != null) {
            if (constraint.minDp.value >= 0f && constraint.maxDp.value >= 0f && constraint.minDp > constraint.maxDp) {
                return ResolutionResult.Invalid("Contradictory layout constraints: minDp (${constraint.minDp}) > maxDp (${constraint.maxDp})")
            }
        }

        val rawResult: ResolutionResult<ResolvedDimension> = when (spec) {
            is DimensionSpec.Fixed -> {
                if (spec.valueDp.value < 0f || spec.valueDp.value.isNaN()) {
                    ResolutionResult.Recovered(ResolvedDimension.Exact(0.dp), "Negative or NaN fixed dimension sanitized to 0.dp")
                } else {
                    ResolutionResult.Resolved(ResolvedDimension.Exact(spec.valueDp))
                }
            }
            is DimensionSpec.Adaptive -> {
                val calculated = AdaptiveScaleCalculator.calculate(
                    availableDimension = available,
                    baseValue = spec.baseDp,
                    policy = dimensionPolicy,
                    minDp = spec.minDp,
                    maxDp = spec.maxDp
                )
                ResolutionResult.Resolved(ResolvedDimension.Exact(calculated))
            }
            is DimensionSpec.Fraction -> {
                if (spec.percentage.isNaN() || spec.percentage.isInfinite()) {
                    ResolutionResult.Recovered(ResolvedDimension.Exact(0.dp), "NaN or Infinity fraction percentage sanitized to 0.dp")
                } else {
                    val pct = spec.percentage.coerceIn(0.0f, 1.0f)
                    val calculated = (available.value * pct).dp
                    if (spec.percentage < 0.0f || spec.percentage > 1.0f) {
                        ResolutionResult.Recovered(ResolvedDimension.Exact(calculated), "Percentage out of bounds 0.0..1.0, clamped")
                    } else {
                        ResolutionResult.Resolved(ResolvedDimension.Exact(calculated))
                    }
                }
            }
            is DimensionSpec.Fill -> ResolutionResult.Resolved(ResolvedDimension.Fill)
            is DimensionSpec.Wrap -> ResolutionResult.Resolved(ResolvedDimension.Wrap)
            is DimensionSpec.Token -> {
                val resolvedToken = tokenResolver?.resolveDimension(spec.tokenKey)
                if (resolvedToken != null) {
                    ResolutionResult.Resolved(ResolvedDimension.Exact(resolvedToken))
                } else {
                    ResolutionResult.Unsupported("Unknown dimension token: ${spec.tokenKey}")
                }
            }
        }

        return when (rawResult) {
            is ResolutionResult.Resolved -> applyConstraints(rawResult.value, constraint) { ResolutionResult.Resolved(it) }
            is ResolutionResult.Recovered -> applyConstraints(rawResult.fallbackValue, constraint) { ResolutionResult.Recovered(it, rawResult.reason) }
            else -> rawResult
        }
    }

    private fun applyConstraints(
        resolved: ResolvedDimension,
        constraint: LayoutConstraintSpec?,
        wrapResult: (ResolvedDimension) -> ResolutionResult<ResolvedDimension>
    ): ResolutionResult<ResolvedDimension> {
        val exact = resolved as? ResolvedDimension.Exact ?: return wrapResult(resolved)
        var resultDp = exact.valueDp

        if (constraint?.minDp != null && constraint.minDp.value >= 0f) {
            if (resultDp < constraint.minDp) resultDp = constraint.minDp
        }
        if (constraint?.maxDp != null && constraint.maxDp.value >= 0f) {
            val effectiveMax = if (constraint.minDp != null && constraint.maxDp < constraint.minDp) constraint.minDp else constraint.maxDp
            if (resultDp > effectiveMax) resultDp = effectiveMax
        }

        val sanitizedDp = if (resultDp.value < 0f || resultDp.value.isNaN()) 0.dp else resultDp
        return wrapResult(ResolvedDimension.Exact(sanitizedDp))
    }
}
