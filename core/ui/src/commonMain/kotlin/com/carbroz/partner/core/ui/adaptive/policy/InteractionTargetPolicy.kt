package com.carbroz.partner.core.ui.adaptive.policy

import com.carbroz.partner.core.ui.adaptive.context.SafeAreaInsets
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Interaction target accessibility policy enforcing minimum touch target hit areas (48dp x 48dp).
 *
 * Visual size remains independent from interaction target padding.
 */
object InteractionTargetPolicy {

    val minTouchTargetDp: Dp = 48.dp

    fun calculatePadding(visualWidth: Dp, visualHeight: Dp): SafeAreaInsets {
        val padW = if (visualWidth < minTouchTargetDp) (minTouchTargetDp - visualWidth) / 2 else 0.dp
        val padH = if (visualHeight < minTouchTargetDp) (minTouchTargetDp - visualHeight) / 2 else 0.dp
        return SafeAreaInsets(top = padH, bottom = padH, start = padW, end = padW)
    }
}
