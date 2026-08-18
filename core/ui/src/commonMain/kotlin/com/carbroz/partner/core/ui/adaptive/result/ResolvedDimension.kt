package com.carbroz.partner.core.ui.adaptive.result

import androidx.compose.runtime.Immutable
import androidx.compose.ui.unit.Dp

@Immutable
sealed interface ResolvedDimension {
    data class Exact(val valueDp: Dp) : ResolvedDimension
    data object Fill : ResolvedDimension
    data object Wrap : ResolvedDimension
}
