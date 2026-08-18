package com.carbroz.partner.core.ui.adaptive

import androidx.compose.runtime.Immutable

/**
 * Parent container layout participation parameters for child nodes.
 *
 * Defines flex weight, fillWeight behavior, and optional aspect ratio constraints.
 */
@Immutable
data class ParentLayoutChildSpec(
    val weight: Float? = null,
    val fillWeight: Boolean = true,
    val aspectRatio: Float? = null
)
