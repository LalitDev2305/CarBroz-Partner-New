package com.carbroz.partner.core.ui.adaptive.spec

import androidx.compose.runtime.Immutable
import androidx.compose.ui.unit.Dp

@Immutable
sealed interface SpacingSpec {
    data class Fixed(val spaceDp: Dp) : SpacingSpec
    data class Adaptive(val baseDp: Dp) : SpacingSpec
    data class Token(val key: String) : SpacingSpec
}
