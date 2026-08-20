package com.carbroz.partner.core.ui.adaptive.spec

import androidx.compose.runtime.Immutable
import androidx.compose.ui.unit.Dp

@Immutable
data class LayoutConstraintSpec(
    val minDp: Dp? = null,
    val maxDp: Dp? = null
) {
    init {
        if (minDp != null) {
            require(minDp.value >= 0f && !minDp.value.isNaN() && !minDp.value.isInfinite()) {
                "minDp must be non-negative and finite"
            }
        }
        if (maxDp != null) {
            require(maxDp.value >= 0f && !maxDp.value.isNaN() && !maxDp.value.isInfinite()) {
                "maxDp must be non-negative and finite"
            }
        }
        if (minDp != null && maxDp != null) {
            require(minDp <= maxDp) {
                "minDp cannot be greater than maxDp"
            }
        }
    }
}
