package com.carbroz.partner.core.ui.adaptive.resolver

import com.carbroz.partner.core.ui.adaptive.result.ResolutionResult
import com.carbroz.partner.core.ui.adaptive.spec.TypographySpec
import com.carbroz.partner.core.ui.tokens.TypographyTokenResolver
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.sp

object TypographyResolver {

    fun resolve(
        spec: TypographySpec,
        tokenResolver: TypographyTokenResolver? = null
    ): ResolutionResult<TextStyle> {
        return when (spec) {
            is TypographySpec.Fixed -> {
                val style = TextStyle(fontSize = spec.sizeSp.sp)
                ResolutionResult.Resolved(style)
            }
            is TypographySpec.Token -> {
                val resolved = tokenResolver?.resolveTypography(spec.key)
                if (resolved != null) {
                    ResolutionResult.Resolved(resolved)
                } else {
                    ResolutionResult.Unsupported(spec.key)
                }
            }
        }
    }
}
