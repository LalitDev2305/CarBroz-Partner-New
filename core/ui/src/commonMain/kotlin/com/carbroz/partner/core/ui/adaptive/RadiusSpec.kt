package com.carbroz.partner.core.ui.adaptive

import androidx.compose.runtime.Immutable
import androidx.compose.ui.unit.Dp

/**
 * Corner radius intent specification.
 */
@Immutable
sealed interface RadiusSpec {
    data class Fixed(val valueDp: Dp) : RadiusSpec
    data class Adaptive(val baseDp: Dp, val minDp: Dp? = null, val maxDp: Dp? = null) : RadiusSpec
    data class Token(val tokenKey: String) : RadiusSpec
    data object Full : RadiusSpec
}
