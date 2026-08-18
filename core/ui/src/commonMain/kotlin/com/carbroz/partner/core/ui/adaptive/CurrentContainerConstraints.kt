package com.carbroz.partner.core.ui.adaptive

import androidx.compose.runtime.Immutable
import androidx.compose.ui.unit.Dp

/**
 * Immediate parent container bounds constraints.
 *
 * Exposes available width and height for container-relative percentage/fraction resolution.
 */
@Immutable
data class CurrentContainerConstraints(
    val availableWidth: Dp,
    val availableHeight: Dp
)
