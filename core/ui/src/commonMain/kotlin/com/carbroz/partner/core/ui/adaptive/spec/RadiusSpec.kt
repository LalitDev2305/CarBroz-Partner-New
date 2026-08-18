package com.carbroz.partner.core.ui.adaptive.spec

import androidx.compose.runtime.Immutable
import androidx.compose.ui.unit.Dp

@Immutable
sealed interface RadiusSpec {
    data class Fixed(val radiusDp: Dp) : RadiusSpec
    data class Token(val key: String) : RadiusSpec
}
