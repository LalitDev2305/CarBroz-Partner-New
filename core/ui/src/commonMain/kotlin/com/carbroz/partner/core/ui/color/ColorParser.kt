package com.carbroz.partner.core.ui.color

import androidx.compose.ui.graphics.Color

public object ColorParser {
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
}
