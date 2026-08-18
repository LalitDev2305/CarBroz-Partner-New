package com.carbroz.partner.core.ui.adaptive

import androidx.compose.runtime.Immutable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Screen-level window metrics snapshot.
 *
 * Owned by platform host observers and distributed down the Compose tree.
 */
enum class WindowWidthClass { COMPACT, MEDIUM, EXPANDED }

@Immutable
data class WindowEnvironment(
    val windowWidth: Dp,
    val windowHeight: Dp,
    val density: Float,
    val fontScale: Float,
    val safeArea: SafeAreaInsets = SafeAreaInsets()
) {
    val isLandscape: Boolean get() = windowWidth > windowHeight
    val widthClass: WindowWidthClass get() = when {
        windowWidth < 600.dp -> WindowWidthClass.COMPACT
        windowWidth < 840.dp -> WindowWidthClass.MEDIUM
        else -> WindowWidthClass.EXPANDED
    }
}
