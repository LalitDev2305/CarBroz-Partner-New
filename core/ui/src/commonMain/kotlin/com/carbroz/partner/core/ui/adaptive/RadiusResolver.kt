package com.carbroz.partner.core.ui.adaptive

import com.carbroz.partner.core.ui.tokens.RadiusTokenResolver
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp

/**
 * Single canonical resolver for corner radius specifications.
 */
object RadiusResolver {

    private val radiusPolicy = AdaptiveScalePolicy(
        referenceDp = DesignReferenceSpace.widthDp,
        dampingExponent = 0.25f,
        minScaleFactor = 0.9f,
        maxScaleFactor = 1.3f
    )

    fun resolve(
        spec: RadiusSpec,
        context: ResolutionContext,
        tokenResolver: RadiusTokenResolver? = null
    ): ResolutionResult<Shape> {
        return when (spec) {
            is RadiusSpec.Fixed -> ResolutionResult.Resolved(RoundedCornerShape(spec.valueDp))
            is RadiusSpec.Adaptive -> {
                val calculated = AdaptiveScaleCalculator.calculate(
                    availableDimension = context.availableDimension,
                    baseValue = spec.baseDp,
                    policy = radiusPolicy,
                    minDp = spec.minDp,
                    maxDp = spec.maxDp
                )
                ResolutionResult.Resolved(RoundedCornerShape(calculated))
            }
            is RadiusSpec.Full -> ResolutionResult.Resolved(RoundedCornerShape(CornerSize(50)))
            is RadiusSpec.Token -> {
                val tokenShape = tokenResolver?.resolveRadius(spec.tokenKey)
                if (tokenShape != null) {
                    ResolutionResult.Resolved(tokenShape)
                } else {
                    ResolutionResult.Unsupported("Unknown radius token: ${spec.tokenKey}")
                }
            }
        }
    }
}
