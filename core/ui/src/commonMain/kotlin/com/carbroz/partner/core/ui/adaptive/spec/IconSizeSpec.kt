package com.carbroz.partner.core.ui.adaptive.spec

import androidx.compose.runtime.Immutable
import androidx.compose.ui.unit.Dp

@Immutable
sealed interface IconSizeSpec {
    data class Fixed(val sizeDp: Dp) : IconSizeSpec
    data class Adaptive(val baseDp: Dp) : IconSizeSpec
    data class Token(val key: String) : IconSizeSpec
}
