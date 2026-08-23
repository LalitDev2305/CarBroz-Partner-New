package com.carbroz.foundation.designsystem

import androidx.compose.foundation.layout.sizeIn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * Guarantees the design-system minimum interaction area for custom interactive
 * renderers while still allowing the visual content itself to be smaller.
 *
 * Material components already provide their own accessibility sizing and should
 * not mechanically add this modifier. It exists for custom Compose/SDUI
 * interactions where the renderer owns the hit target.
 */
@Composable
fun Modifier.carBrozMinimumInteractionSize(): Modifier {
    val minimumSize = CarBrozDesignSystem.interactionSizing.minimumTarget
    return sizeIn(minWidth = minimumSize, minHeight = minimumSize)
}
