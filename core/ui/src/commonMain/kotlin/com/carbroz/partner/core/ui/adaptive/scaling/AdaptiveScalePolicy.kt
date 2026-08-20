package com.carbroz.partner.core.ui.adaptive.scaling

import androidx.compose.runtime.Immutable
import androidx.compose.ui.unit.Dp

/**
 * Property-specific scaling policy configuration.
 *
 * Defines reference dimension baseline, damping exponent, and minimum/maximum scale factor limits.
 */
@Immutable
data class AdaptiveScalePolicy(
    val referenceDp: Dp,
    val dampingExponent: Float = 0.5f,
    val minScaleFactor: Float = 0.8f,
    val maxScaleFactor: Float = 2.0f
) {
    init {
        require(referenceDp.value > 0f && !referenceDp.value.isNaN() && !referenceDp.value.isInfinite()) {
            "referenceDp must be positive and finite"
        }
        require(dampingExponent >= 0f && !dampingExponent.isNaN() && !dampingExponent.isInfinite()) {
            "dampingExponent must be non-negative and finite"
        }
        require(minScaleFactor >= 0f && !minScaleFactor.isNaN() && !minScaleFactor.isInfinite()) {
            "minScaleFactor must be non-negative and finite"
        }
        require(maxScaleFactor >= 0f && !maxScaleFactor.isNaN() && !maxScaleFactor.isInfinite()) {
            "maxScaleFactor must be non-negative and finite"
        }
        require(minScaleFactor <= maxScaleFactor) {
            "minScaleFactor must be <= maxScaleFactor"
        }
    }
}
