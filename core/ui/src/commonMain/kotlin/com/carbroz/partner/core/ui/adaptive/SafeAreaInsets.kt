package com.carbroz.partner.core.ui.adaptive

import androidx.compose.runtime.Immutable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Immutable representation of safe area layout insets.
 *
 * Exposes top, bottom, start, and end insets to protect content from system bars, display cutouts, and notches.
 */
@Immutable
data class SafeAreaInsets(
    val top: Dp = 0.dp,
    val bottom: Dp = 0.dp,
    val start: Dp = 0.dp,
    val end: Dp = 0.dp
)
