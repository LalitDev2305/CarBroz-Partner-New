package com.carbroz.partner.core.ui.adaptive

import androidx.compose.runtime.Immutable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Single canonical owner of reference dimensions for adaptive scaling calculations.
 *
 * Establishes baseline design reference space (360dp width x 640dp height) without platform branching.
 */
@Immutable
object DesignReferenceSpace {
    val widthDp: Dp = 360.dp
    val heightDp: Dp = 640.dp
}
