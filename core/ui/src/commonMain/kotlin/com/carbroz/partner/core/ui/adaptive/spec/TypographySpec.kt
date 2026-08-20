package com.carbroz.partner.core.ui.adaptive.spec

import androidx.compose.runtime.Immutable

@Immutable
sealed interface TypographySpec {
    data class Fixed(val sizeSp: Float) : TypographySpec {
        init {
            require(sizeSp > 0f && !sizeSp.isNaN() && !sizeSp.isInfinite()) {
                "Typography sizeSp must be positive and finite"
            }
        }
    }

    data class Token(val key: String) : TypographySpec {
        init {
            require(key.isNotBlank()) {
                "Token key cannot be blank"
            }
        }
    }
}
