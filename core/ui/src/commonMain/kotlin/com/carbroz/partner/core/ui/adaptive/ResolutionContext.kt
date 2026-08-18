package com.carbroz.partner.core.ui.adaptive

import androidx.compose.runtime.Immutable
import androidx.compose.ui.unit.Dp

/**
 * Contextual evaluation snapshot for property resolution.
 *
 * Combines target evaluation axis, screen-level window environment, and immediate parent container constraints.
 */
@Immutable
data class ResolutionContext(
    val axis: ResolutionAxis,
    val window: WindowEnvironment,
    val container: CurrentContainerConstraints
) {
    val availableDimension: Dp get() = when (axis) {
        ResolutionAxis.HORIZONTAL -> container.availableWidth
        ResolutionAxis.VERTICAL -> container.availableHeight
    }
}
