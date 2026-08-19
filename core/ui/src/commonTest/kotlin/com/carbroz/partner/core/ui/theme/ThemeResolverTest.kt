package com.carbroz.partner.core.ui.theme

import androidx.compose.ui.graphics.Color
import kotlin.test.Test
import kotlin.test.assertEquals

class ThemeResolverTest {

    @Test
    fun testParseHexColor6Char() {
        val color = ThemeResolver.parseHexColor("#FF0000")
        assertEquals(Color(0xFFFF0000), color)
    }

    @Test
    fun testParseHexColor8Char() {
        val color = ThemeResolver.parseHexColor("#80FF0000")
        assertEquals(Color(0x80FF0000), color)
    }

    @Test
    fun testParseHexColorFallback() {
        val fallback = Color.Blue
        assertEquals(fallback, ThemeResolver.parseHexColor("invalid", fallback))
        assertEquals(fallback, ThemeResolver.parseHexColor(null, fallback))
    }
}
