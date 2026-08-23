package com.carbroz.foundation.designsystem

import androidx.compose.runtime.Immutable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Product-neutral guidance for constraining readable content on large surfaces.
 *
 * Templates and renderers may use this policy to decide how much horizontal
 * space a content region should consume. It does not encode product screens or
 * SDUI semantics.
 */
@Immutable
data class ContentLayoutPolicy(
    val maxReadableWidth: Dp,
    val horizontalMargin: Dp,
) {
    init {
        require(maxReadableWidth > 0.dp) { "Maximum readable width must be positive." }
        require(horizontalMargin >= 0.dp) { "Horizontal margin must not be negative." }
    }

    companion object {
        val Compact = ContentLayoutPolicy(
            maxReadableWidth = 600.dp,
            horizontalMargin = 16.dp,
        )
        val Medium = ContentLayoutPolicy(
            maxReadableWidth = 720.dp,
            horizontalMargin = 24.dp,
        )
        val Expanded = ContentLayoutPolicy(
            maxReadableWidth = 840.dp,
            horizontalMargin = 32.dp,
        )
    }
}

object ContentLayoutPolicyResolver {
    fun forWidthClass(widthClass: AdaptiveWidthClass): ContentLayoutPolicy = when (widthClass) {
        AdaptiveWidthClass.Compact -> ContentLayoutPolicy.Compact
        AdaptiveWidthClass.Medium -> ContentLayoutPolicy.Medium
        AdaptiveWidthClass.Expanded -> ContentLayoutPolicy.Expanded
    }
}
