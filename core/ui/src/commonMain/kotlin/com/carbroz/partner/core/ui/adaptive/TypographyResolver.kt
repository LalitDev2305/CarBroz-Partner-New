package com.carbroz.partner.core.ui.adaptive

import com.carbroz.partner.core.ui.tokens.TypographyTokenResolver
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp

/**
 * Single canonical resolver for typography specifications.
 *
 * System accessibility fontScale is applied natively by Compose `sp` without double-scaling.
 */
object TypographyResolver {

    fun resolve(
        spec: TypographySpec,
        tokenResolver: TypographyTokenResolver? = null
    ): ResolutionResult<TextStyle> {
        return when (spec) {
            is TypographySpec.Fixed -> {
                val fontSp = spec.fontSizeSp.sp
                val lineSp = spec.lineHeightSp?.sp ?: TextUnit.Unspecified
                ResolutionResult.Resolved(TextStyle(fontSize = fontSp, lineHeight = lineSp))
            }
            is TypographySpec.Token -> {
                val tokenStyle = tokenResolver?.resolveTypography(spec.tokenKey)
                if (tokenStyle != null) {
                    ResolutionResult.Resolved(tokenStyle)
                } else {
                    ResolutionResult.Unsupported("Unknown typography token: ${spec.tokenKey}")
                }
            }
        }
    }
}
