package com.carbroz.partner.core.ui.adaptive.scaling

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.pow

/**
 * Pure mathematical calculator for adaptive scaling.
 *
 * Implements deterministic scaling: scale = (available / reference) ^ dampingExponent, coerced to policy scale bounds.
 */
object AdaptiveScaleCalculator {

    fun calculate(
        availableDimension: Dp,
        baseValue: Dp,
        policy: AdaptiveScalePolicy,
        minDp: Dp? = null,
        maxDp: Dp? = null
    ): Dp {
        val avail = availableDimension.value
        if (avail <= 0f || avail.isNaN() || avail.isInfinite()) {
            return clamp(baseValue, minDp, maxDp)
        }

        val ref = policy.referenceDp.value
        val base = baseValue.value
        val ratio = (avail / ref).toDouble()
        val rawScale = ratio.pow(policy.dampingExponent.toDouble()).toFloat()
        val clampedScale = rawScale.coerceIn(policy.minScaleFactor, policy.maxScaleFactor)
        val calculated = (base * clampedScale).dp

        return clamp(calculated, minDp, maxDp)
    }

    private fun clamp(value: Dp, minDp: Dp?, maxDp: Dp?): Dp {
        var result = value
        if (minDp != null && result < minDp) {
            result = minDp
        }
        if (maxDp != null && result > maxDp) {
            result = maxDp
        }
        return result
    }
}
