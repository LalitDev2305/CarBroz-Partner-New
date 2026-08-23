package com.carbroz.foundation.adaptive

import androidx.compose.runtime.Immutable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** Product-neutral readable-content guidance derived from adaptive width class. */
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
        val Compact = ContentLayoutPolicy(600.dp, 16.dp)
        val Medium = ContentLayoutPolicy(720.dp, 24.dp)
        val Expanded = ContentLayoutPolicy(840.dp, 32.dp)
    }
}

object ContentLayoutPolicyResolver {
    fun forWidthClass(widthClass: AdaptiveWidthClass): ContentLayoutPolicy = when (widthClass) {
        AdaptiveWidthClass.Compact -> ContentLayoutPolicy.Compact
        AdaptiveWidthClass.Medium -> ContentLayoutPolicy.Medium
        AdaptiveWidthClass.Expanded -> ContentLayoutPolicy.Expanded
    }
}
