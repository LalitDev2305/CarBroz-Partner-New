package com.carbroz.foundation.designsystem

import androidx.compose.runtime.Immutable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Shared accessibility-oriented interaction sizing.
 *
 * Renderers should keep visual size and interactive hit target conceptually
 * separate so compact layouts never require undersized touch/click targets.
 */
@Immutable
data class InteractionSizing(
    val minimumTarget: Dp,
) {
    init {
        require(minimumTarget.value > 0f) { "Minimum interaction target must be positive." }
    }

    companion object {
        val Default = InteractionSizing(minimumTarget = 48.dp)
    }
}
