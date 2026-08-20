package com.carbroz.partner.core.ui.adaptive.spec

import androidx.compose.runtime.Immutable
import androidx.compose.ui.unit.Dp

@Immutable
sealed interface SpacingSpec {
    data class Fixed(val spaceDp: Dp) : SpacingSpec {
        init {
            require(spaceDp.value >= 0f && !spaceDp.value.isNaN() && !spaceDp.value.isInfinite()) {
                "Fixed spacing must be non-negative and finite"
            }
        }
    }

    data class Adaptive(val baseDp: Dp) : SpacingSpec {
        init {
            require(baseDp.value >= 0f && !baseDp.value.isNaN() && !baseDp.value.isInfinite()) {
                "Adaptive baseDp must be non-negative and finite"
            }
        }
    }

    data class Token(val key: String) : SpacingSpec {
        init {
            require(key.isNotBlank()) {
                "Token key cannot be blank"
            }
        }
    }
}
