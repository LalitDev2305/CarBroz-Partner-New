package com.carbroz.partner.core.ui.tokens

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp

/**
 * Focused typed token resolution interfaces decoupling visual token queries from god resolvers.
 */
interface ColorTokenResolver {
    fun resolveColor(key: String): Color?
}

interface DimensionTokenResolver {
    fun resolveDimension(key: String): Dp?
}

interface SpacingTokenResolver {
    fun resolveSpacing(key: String): Dp?
}

interface TypographyTokenResolver {
    fun resolveTypography(key: String): TextStyle?
}

interface RadiusTokenResolver {
    fun resolveRadius(key: String): Shape?
}

interface IconTokenResolver {
    fun resolveIconSize(key: String): Dp?
}
