package com.carbroz.partner.core.ui.adaptive.context

import androidx.compose.runtime.Immutable

@Immutable
data class ResolutionContext(
    val axis: ResolutionAxis,
    val container: CurrentContainerConstraints
)
