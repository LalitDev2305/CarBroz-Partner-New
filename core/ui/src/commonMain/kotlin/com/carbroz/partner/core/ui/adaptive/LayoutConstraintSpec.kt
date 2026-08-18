package com.carbroz.partner.core.ui.adaptive

import androidx.compose.runtime.Immutable
import androidx.compose.ui.unit.Dp

/**
 * Size constraint parameters applied to dimension resolution.
 *
 * Defines explicit minDp, maxDp, and optional preferredDp bounds.
 */
@Immutable
data class LayoutConstraintSpec(
    val minDp: Dp? = null,
    val maxDp: Dp? = null,
    val preferredDp: Dp? = null
)
