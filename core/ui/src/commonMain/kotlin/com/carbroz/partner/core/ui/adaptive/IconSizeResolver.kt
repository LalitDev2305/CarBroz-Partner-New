package com.carbroz.partner.core.ui.adaptive

import com.carbroz.partner.core.ui.tokens.IconTokenResolver
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Single canonical resolver for icon size specifications.
 */
object IconSizeResolver {

    private val iconPolicy = AdaptiveScalePolicy(
        referenceDp = DesignReferenceSpace.widthDp,
        dampingExponent = 0.25f,
        minScaleFactor = 0.9f,
        maxScaleFactor = 1.4f
    )

    fun resolve(
        spec: IconSizeSpec,
        context: ResolutionContext,
        tokenResolver: IconTokenResolver? = null
    ): ResolutionResult<Dp> {
        return when (spec) {
            is IconSizeSpec.Fixed -> ResolutionResult.Resolved(spec.valueDp)
            is IconSizeSpec.Adaptive -> {
                val calculated = AdaptiveScaleCalculator.calculate(
                    availableDimension = context.availableDimension,
                    baseValue = spec.baseDp,
                    policy = iconPolicy,
                    minDp = spec.minDp,
                    maxDp = spec.maxDp
                )
                ResolutionResult.Resolved(calculated)
            }
            is IconSizeSpec.Token -> {
                val tokenDp = tokenResolver?.resolveIconSize(spec.tokenKey)
                if (tokenDp != null) {
                    ResolutionResult.Resolved(tokenDp)
                } else {
                    ResolutionResult.Unsupported("Unknown icon size token: ${spec.tokenKey}")
                }
            }
        }
    }
}
