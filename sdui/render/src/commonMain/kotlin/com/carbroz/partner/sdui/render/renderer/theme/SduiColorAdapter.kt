package com.carbroz.partner.sdui.render.renderer.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

public object SduiColorAdapter {
    @Composable
    public fun parseColor(colorString: String?, fallback: Color = MaterialTheme.colorScheme.primary): Color {
        if (colorString.isNullOrBlank()) return fallback
        if (colorString.startsWith("#")) {
            val hex = colorString.removePrefix("#")
            val parsed = when (hex.length) {
                6 -> hex.toLongOrNull(16)?.let { 0xFF000000 or it }
                8 -> hex.toLongOrNull(16)
                else -> null
            }
            if (parsed != null) return Color(parsed)
        }
        return fallback
    }
}
