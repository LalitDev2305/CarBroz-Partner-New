package com.carbroz.partner.core.ui.adaptive.spec

import androidx.compose.runtime.Immutable

@Immutable
data class ParentLayoutChildSpec(
    val widthSpec: DimensionSpec,
    val heightSpec: DimensionSpec
)
