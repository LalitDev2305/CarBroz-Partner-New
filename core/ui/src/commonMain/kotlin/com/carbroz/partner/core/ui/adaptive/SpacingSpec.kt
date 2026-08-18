package com.carbroz.partner.core.ui.adaptive

import androidx.compose.runtime.Immutable
import androidx.compose.ui.unit.Dp

/**
 * Spacing intent specification (padding, margin, gap).
 */
@Immutable
sealed interface SpacingSpec {
    data class Fixed(val valueDp: Dp) : SpacingSpec
    data class Adaptive(val baseDp: Dp, val minDp: Dp? = null, val maxDp: Dp? = null) : SpacingSpec
    data class Fraction(val percentage: Float) : SpacingSpec
    data class Token(val tokenKey: String) : SpacingSpec
}
