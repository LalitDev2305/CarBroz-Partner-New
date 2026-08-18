package com.carbroz.partner.core.ui.adaptive.spec

import androidx.compose.runtime.Immutable

@Immutable
sealed interface TypographySpec {
    data class Fixed(val sizeSp: Float) : TypographySpec
    data class Token(val key: String) : TypographySpec
}
