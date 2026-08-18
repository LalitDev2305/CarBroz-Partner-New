package com.carbroz.partner.core.ui.adaptive

import com.carbroz.partner.core.ui.tokens.SpacingTokenResolver
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Single canonical resolver for spacing specifications (padding, margin, gap).
 */
object SpacingResolver {

    private val spacingPolicy = AdaptiveScalePolicy(
        referenceDp = DesignReferenceSpace.widthDp,
        dampingExponent = 0.35f,
        minScaleFactor = 0.9f,
        maxScaleFactor = 1.5f
    )

    fun resolve(
        spec: SpacingSpec,
        context: ResolutionContext,
        tokenResolver: SpacingTokenResolver? = null
    ): ResolutionResult<Dp> {
        val available = context.availableDimension

        return when (spec) {
            is SpacingSpec.Fixed -> {
                if (spec.valueDp.value < 0f || spec.valueDp.value.isNaN()) {
                    ResolutionResult.Recovered(0.dp, "Negative or NaN spacing sanitized to 0.dp")
                } else {
                    ResolutionResult.Resolved(spec.valueDp)
                }
            }
            is SpacingSpec.Adaptive -> {
                val calculated = AdaptiveScaleCalculator.calculate(
                    availableDimension = available,
                    baseValue = spec.baseDp,
                    policy = spacingPolicy,
                    minDp = spec.minDp,
                    maxDp = spec.maxDp
                )
                ResolutionResult.Resolved(calculated)
            }
            is SpacingSpec.Fraction -> {
                val pct = spec.percentage.coerceIn(0.0f, 1.0f)
                val calculated = (available.value * pct).dp
                ResolutionResult.Resolved(calculated)
            }
            is SpacingSpec.Token -> {
                val tokenDp = tokenResolver?.resolveSpacing(spec.tokenKey)
                if (tokenDp != null) {
                    ResolutionResult.Resolved(tokenDp)
                } else {
                    ResolutionResult.Unsupported("Unknown spacing token: ${spec.tokenKey}")
                }
            }
        }
    }
}
