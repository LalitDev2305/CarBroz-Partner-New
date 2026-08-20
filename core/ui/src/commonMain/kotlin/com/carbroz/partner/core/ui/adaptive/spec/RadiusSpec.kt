package com.carbroz.partner.core.ui.adaptive.spec

import androidx.compose.runtime.Immutable
import androidx.compose.ui.unit.Dp

@Immutable
sealed interface RadiusSpec {
    data class Fixed(val radiusDp: Dp) : RadiusSpec {
        init {
            require(radiusDp.value >= 0f && !radiusDp.value.isNaN() && !radiusDp.value.isInfinite()) {
                "Fixed radius must be non-negative and finite"
            }
        }
    }

    data class Token(val key: String) : RadiusSpec {
        init {
            require(key.isNotBlank()) {
                "Token key cannot be blank"
            }
        }
    }
}
