package com.carbroz.partner.core.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.sp

/**
 * Pure platform-neutral adapter resolving Hex colors and typography specifications safely.
 */
public object ThemeResolver {
    public fun parseHexColor(colorHex: String?, fallback: Color = Color.Unspecified): Color {
        if (colorHex.isNullOrBlank()) return fallback
        val cleanHex = colorHex.removePrefix("#").trim()
        return try {
            when (cleanHex.length) {
                6 -> Color(cleanHex.toLong(16) or 0xFF000000)
                8 -> Color(cleanHex.toLong(16))
                else -> fallback
            }
        } catch (_: Exception) {
            fallback
        }
    }

    public fun resolveTextStyle(
        fontSizeSp: Float?,
        fontWeight: String?,
        colorHex: String?,
        defaultStyle: TextStyle = TextStyle.Default
    ): TextStyle {
        val color = parseHexColor(colorHex, defaultStyle.color)
        val fontSize = fontSizeSp?.let { it.sp } ?: defaultStyle.fontSize
        return defaultStyle.copy(color = color, fontSize = fontSize)
    }
}
