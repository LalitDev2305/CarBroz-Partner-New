package com.carbroz.partner.core.ui.adaptive.spec

import androidx.compose.runtime.Immutable
import androidx.compose.ui.unit.Dp

@Immutable
sealed interface DimensionSpec {
    data class Fixed(val valueDp: Dp) : DimensionSpec {
        init {
            require(valueDp.value >= 0f && !valueDp.value.isNaN() && !valueDp.value.isInfinite()) {
                "Fixed dimension must be non-negative and finite"
            }
        }
    }

    data class Adaptive(
        val baseDp: Dp,
        val minDp: Dp? = null,
        val maxDp: Dp? = null
    ) : DimensionSpec {
        init {
            require(baseDp.value >= 0f && !baseDp.value.isNaN() && !baseDp.value.isInfinite()) {
                "Adaptive baseDp must be non-negative and finite"
            }
            if (minDp != null) {
                require(minDp.value >= 0f && !minDp.value.isNaN() && !minDp.value.isInfinite()) {
                    "minDp must be non-negative and finite"
                }
            }
            if (maxDp != null) {
                require(maxDp.value >= 0f && !maxDp.value.isNaN() && !maxDp.value.isInfinite()) {
                    "maxDp must be non-negative and finite"
                }
            }
            if (minDp != null && maxDp != null) {
                require(minDp <= maxDp) {
                    "minDp must be <= maxDp"
                }
            }
        }
    }

    data class Fraction(val percentage: Float) : DimensionSpec {
        init {
            require(percentage in 0f..1f && !percentage.isNaN() && !percentage.isInfinite()) {
                "Fraction percentage must be in range [0, 1] and finite"
            }
        }
    }

    data object Fill : DimensionSpec

    data object Wrap : DimensionSpec

    data class Token(val key: String) : DimensionSpec {
        init {
            require(key.isNotBlank()) {
                "Token key cannot be blank"
            }
        }
    }
}
