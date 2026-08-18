package com.carbroz.partner.core.ui.adaptive

import androidx.compose.runtime.Immutable

/**
 * Typography sizing intent specification.
 *
 * Expressed in sp. Native system accessibility fontScale is applied by Compose runtime without double-scaling.
 */
@Immutable
sealed interface TypographySpec {
    data class Fixed(val fontSizeSp: Float, val lineHeightSp: Float? = null) : TypographySpec
    data class Token(val tokenKey: String) : TypographySpec
}
