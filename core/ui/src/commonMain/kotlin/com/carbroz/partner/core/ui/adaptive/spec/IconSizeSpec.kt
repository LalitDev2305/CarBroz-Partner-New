package com.carbroz.partner.core.ui.adaptive.spec

import androidx.compose.runtime.Immutable
import androidx.compose.ui.unit.Dp

@Immutable
sealed interface IconSizeSpec {
    data class Fixed(val sizeDp: Dp) : IconSizeSpec {
        init {
            require(sizeDp.value >= 0f && !sizeDp.value.isNaN() && !sizeDp.value.isInfinite()) {
                "Fixed icon size must be non-negative and finite"
            }
        }
    }

    data class Adaptive(val baseDp: Dp) : IconSizeSpec {
        init {
            require(baseDp.value >= 0f && !baseDp.value.isNaN() && !baseDp.value.isInfinite()) {
                "Adaptive baseDp must be non-negative and finite"
            }
        }
    }

    data class Token(val key: String) : IconSizeSpec {
        init {
            require(key.isNotBlank()) {
                "Token key cannot be blank"
            }
        }
    }
}
