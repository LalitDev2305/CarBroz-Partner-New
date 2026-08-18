package com.carbroz.partner.core.ui.adaptive.context

import androidx.compose.runtime.Immutable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

enum class WindowWidthClass {
    COMPACT,
    MEDIUM,
    EXPANDED
}

@Immutable
data class WindowEnvironment(
    val windowWidth: Dp,
    val windowHeight: Dp,
    val density: Float = 1.0f,
    val fontScale: Float = 1.0f
) {
    val widthClass: WindowWidthClass
        get() = when {
            windowWidth < 600.dp -> WindowWidthClass.COMPACT
            windowWidth < 840.dp -> WindowWidthClass.MEDIUM
            else -> WindowWidthClass.EXPANDED
        }
}
