package com.carbroz.partner.core.ui.adaptive.resolver

import com.carbroz.partner.core.ui.adaptive.context.ResolutionContext
import com.carbroz.partner.core.ui.adaptive.result.ResolutionResult
import com.carbroz.partner.core.ui.adaptive.spec.RadiusSpec
import com.carbroz.partner.core.ui.tokens.RadiusTokenResolver
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Shape

object RadiusResolver {

    fun resolve(
        spec: RadiusSpec,
        context: ResolutionContext,
        tokenResolver: RadiusTokenResolver? = null
    ): ResolutionResult<Shape> {
        return when (spec) {
            is RadiusSpec.Fixed -> ResolutionResult.Resolved(RoundedCornerShape(spec.radiusDp))
            is RadiusSpec.Token -> {
                val resolved = tokenResolver?.resolveRadius(spec.key)
                if (resolved != null) {
                    ResolutionResult.Resolved(resolved)
                } else {
                    ResolutionResult.Unsupported(spec.key)
                }
            }
        }
    }
}
