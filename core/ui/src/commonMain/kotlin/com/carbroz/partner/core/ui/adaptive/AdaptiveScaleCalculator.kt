package com.carbroz.partner.core.ui.adaptive

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.pow

/**
 * Pure mathematical calculator for adaptive scaling.
 *
 * Implements deterministic scaling: scale = (available / reference) ^ dampingExponent, coerced to policy scale bounds.
 * Safe against zero, negative, NaN, and Infinity inputs.
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
        val ref = policy.referenceDp.value
        val base = baseValue.value

        val minFactor = if (policy.minScaleFactor.isNaN() || policy.minScaleFactor.isInfinite() || policy.minScaleFactor < 0f) 0.8f else policy.minScaleFactor
        val maxFactor = if (policy.maxScaleFactor.isNaN() || policy.maxScaleFactor.isInfinite() || policy.maxScaleFactor < 0f) 2.0f else policy.maxScaleFactor
        val effectiveMinFactor = if (minFactor > maxFactor) maxFactor else minFactor

        val damp = if (policy.dampingExponent.isNaN() || policy.dampingExponent.isInfinite() || policy.dampingExponent < 0f) 0.5f else policy.dampingExponent

        if (avail.isNaN() || avail.isInfinite() || avail <= 0f || ref <= 0f || ref.isNaN() || ref.isInfinite() || base <= 0f || base.isNaN() || base.isInfinite()) {
            val fallback = if (base > 0f && !base.isNaN() && !base.isInfinite()) base.dp else 0.dp
            return clamp(fallback, minDp, maxDp)
        }

        val ratio = (avail / ref).toDouble()
        val rawScale = ratio.pow(damp.toDouble()).toFloat()
        val clampedScale = if (rawScale.isNaN() || rawScale.isInfinite()) 1.0f else rawScale.coerceIn(effectiveMinFactor, maxFactor)
        val calculated = (base * clampedScale).dp

        return clamp(calculated, minDp, maxDp)
    }

    private fun clamp(value: Dp, minDp: Dp?, maxDp: Dp?): Dp {
        var result = value
        if (minDp != null && minDp.value >= 0f && !minDp.value.isNaN() && !minDp.value.isInfinite()) {
            if (result < minDp) result = minDp
        }
        if (maxDp != null && maxDp.value >= 0f && !maxDp.value.isNaN() && !maxDp.value.isInfinite()) {
            val effectiveMax = if (minDp != null && maxDp < minDp) minDp else maxDp
            if (result > effectiveMax) result = effectiveMax
        }
        return if (result.value < 0f || result.value.isNaN() || result.value.isInfinite()) 0.dp else result
    }
}
