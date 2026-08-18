package com.carbroz.partner.core.ui.adaptive

import androidx.compose.runtime.Immutable
import androidx.compose.ui.unit.Dp

/**
 * Icon size intent specification.
 */
@Immutable
sealed interface IconSizeSpec {
    data class Fixed(val valueDp: Dp) : IconSizeSpec
    data class Adaptive(val baseDp: Dp, val minDp: Dp? = null, val maxDp: Dp? = null) : IconSizeSpec
    data class Token(val tokenKey: String) : IconSizeSpec
}
