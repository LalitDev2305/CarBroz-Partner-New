package com.carbroz.foundation.designsystem

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Shared implementation-level design system used by Compose renderers.
 *
 * This module deliberately does not model SDUI semantic nodes. Template,
 * component and ChildData contracts remain owned by the SDUI layer; renderers
 * may consume these tokens when translating those contracts into Compose UI.
 */
@Composable
fun CarBrozTheme(
    darkTheme: Boolean = false,
    content: @Composable () -> Unit,
) {
    val colors = if (darkTheme) darkColorScheme() else lightColorScheme()

    CompositionLocalProvider(
        LocalCarBrozSpacing provides CarBrozSpacing.Default,
        LocalInteractionSizing provides InteractionSizing.Default,
    ) {
        MaterialTheme(
            colorScheme = colors,
            typography = CarBrozTypography.Default,
            content = content,
        )
    }
}

@Immutable
data class CarBrozSpacing(
    val none: Dp,
    val extraSmall: Dp,
    val small: Dp,
    val medium: Dp,
    val large: Dp,
    val extraLarge: Dp,
) {
    companion object {
        val Default = CarBrozSpacing(
            none = 0.dp,
            extraSmall = 4.dp,
            small = 8.dp,
            medium = 16.dp,
            large = 24.dp,
            extraLarge = 32.dp,
        )
    }
}

val LocalCarBrozSpacing = staticCompositionLocalOf { CarBrozSpacing.Default }
val LocalInteractionSizing = staticCompositionLocalOf { InteractionSizing.Default }

object CarBrozDesignSystem {
    val spacing: CarBrozSpacing
        @Composable get() = LocalCarBrozSpacing.current

    val interactionSizing: InteractionSizing
        @Composable get() = LocalInteractionSizing.current
}
