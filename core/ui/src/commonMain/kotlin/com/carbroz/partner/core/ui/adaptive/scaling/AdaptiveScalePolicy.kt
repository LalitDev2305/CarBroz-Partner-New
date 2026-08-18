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
)
