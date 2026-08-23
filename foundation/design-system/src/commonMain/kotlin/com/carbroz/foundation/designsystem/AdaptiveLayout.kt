package com.carbroz.foundation.designsystem

import androidx.compose.runtime.Immutable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Product-neutral size classes derived from the actual available Compose size.
 *
 * These classes are intentionally device-agnostic: callers pass the current
 * available width/height and receive semantic layout guidance. No phone/tablet
 * or platform-name branching belongs here.
 */
enum class AdaptiveWidthClass {
    Compact,
    Medium,
    Expanded,
}

enum class AdaptiveHeightClass {
    Compact,
    Medium,
    Expanded,
}

@Immutable
data class AdaptiveLayoutInfo(
    val widthClass: AdaptiveWidthClass,
    val heightClass: AdaptiveHeightClass,
    val width: Dp,
    val height: Dp,
)

/** Pure classifier so adaptive behavior is deterministic and unit-testable. */
object AdaptiveLayoutClassifier {
    val MediumWidthBreakpoint: Dp = 600.dp
    val ExpandedWidthBreakpoint: Dp = 840.dp
    val MediumHeightBreakpoint: Dp = 480.dp
    val ExpandedHeightBreakpoint: Dp = 900.dp

    fun classify(width: Dp, height: Dp): AdaptiveLayoutInfo {
        require(width != Dp.Unspecified && width.value.isFinite()) {
            "Available width must be a specified finite value."
        }
        require(height != Dp.Unspecified && height.value.isFinite()) {
            "Available height must be a specified finite value."
        }
        require(width >= 0.dp) { "Available width must not be negative." }
        require(height >= 0.dp) { "Available height must not be negative." }

        return AdaptiveLayoutInfo(
            widthClass = when {
                width < MediumWidthBreakpoint -> AdaptiveWidthClass.Compact
                width < ExpandedWidthBreakpoint -> AdaptiveWidthClass.Medium
                else -> AdaptiveWidthClass.Expanded
            },
            heightClass = when {
                height < MediumHeightBreakpoint -> AdaptiveHeightClass.Compact
                height < ExpandedHeightBreakpoint -> AdaptiveHeightClass.Medium
                else -> AdaptiveHeightClass.Expanded
            },
            width = width,
            height = height,
        )
    }
}
