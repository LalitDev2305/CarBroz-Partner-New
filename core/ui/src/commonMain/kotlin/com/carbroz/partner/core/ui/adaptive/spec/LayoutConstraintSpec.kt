package com.carbroz.partner.core.ui.adaptive.spec

import androidx.compose.runtime.Immutable
import androidx.compose.ui.unit.Dp

@Immutable
data class LayoutConstraintSpec(
    val minDp: Dp? = null,
    val maxDp: Dp? = null
)
