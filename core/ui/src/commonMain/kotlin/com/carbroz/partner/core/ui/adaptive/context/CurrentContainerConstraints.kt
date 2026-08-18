package com.carbroz.partner.core.ui.adaptive.context

import androidx.compose.runtime.Immutable
import androidx.compose.ui.unit.Dp

@Immutable
data class CurrentContainerConstraints(
    val availableWidth: Dp,
    val availableHeight: Dp
)
