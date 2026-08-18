package com.carbroz.partner.core.ui.adaptive

import androidx.compose.runtime.Immutable
import androidx.compose.ui.unit.Dp

/**
 * Resolved dimension model preserving explicit layout intent semantics without Dp value collapse.
 */
@Immutable
sealed interface ResolvedDimension {
    data class Exact(val valueDp: Dp) : ResolvedDimension
    data object Fill : ResolvedDimension
    data object Wrap : ResolvedDimension
}
