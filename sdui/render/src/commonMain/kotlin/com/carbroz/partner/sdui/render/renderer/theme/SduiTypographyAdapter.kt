package com.carbroz.partner.sdui.render.renderer.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.TextStyle

public object SduiTypographyAdapter {
    @Composable
    public fun parseTypography(styleKey: String?): TextStyle = when (styleKey?.lowercase()?.trim()) {
        "heading_lg", "headinglg" -> MaterialTheme.typography.headlineLarge
        "heading_md", "headingmd" -> MaterialTheme.typography.headlineMedium
        "heading_sm", "headingsm" -> MaterialTheme.typography.titleLarge
        "title" -> MaterialTheme.typography.titleMedium
        "body_sm", "bodysm" -> MaterialTheme.typography.bodySmall
        else -> MaterialTheme.typography.bodyMedium
    }
}
