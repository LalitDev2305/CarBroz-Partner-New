package com.carbroz.foundation.designsystem

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.Dp

/**
 * Compose-facing adaptive environment derived exclusively from the available
 * layout constraints. No platform, model, orientation, or device-name checks
 * are involved.
 */
@Immutable
data class AdaptiveEnvironment(
    val width: Dp,
    val height: Dp,
    val widthClass: AdaptiveWidthClass,
    val heightClass: AdaptiveHeightClass,
)

val LocalAdaptiveEnvironment = staticCompositionLocalOf<AdaptiveEnvironment> {
    error("AdaptiveEnvironment is not available outside AdaptiveLayoutProvider.")
}

/**
 * Provides the current adaptive environment to descendants using the actual
 * constraints of the container in which the UI is being rendered.
 */
@Composable
fun AdaptiveLayoutProvider(
    content: @Composable () -> Unit,
) {
    BoxWithConstraints {
        val environment = AdaptiveEnvironment(
            width = maxWidth,
            height = maxHeight,
            widthClass = AdaptiveLayoutClassifier.widthClass(maxWidth),
            heightClass = AdaptiveLayoutClassifier.heightClass(maxHeight),
        )

        LocalAdaptiveEnvironment provides environment
        content()
    }
}
