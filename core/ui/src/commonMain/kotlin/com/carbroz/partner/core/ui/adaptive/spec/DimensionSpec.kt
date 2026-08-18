package com.carbroz.partner.core.ui.adaptive.spec

import androidx.compose.runtime.Immutable
import androidx.compose.ui.unit.Dp

@Immutable
sealed interface DimensionSpec {
    data class Fixed(val valueDp: Dp) : DimensionSpec
    data class Adaptive(val baseDp: Dp, val minDp: Dp? = null, val maxDp: Dp? = null) : DimensionSpec
    data class Fraction(val percentage: Float) : DimensionSpec
    data object Fill : DimensionSpec
    data object Wrap : DimensionSpec
    data class Token(val key: String) : DimensionSpec
}
