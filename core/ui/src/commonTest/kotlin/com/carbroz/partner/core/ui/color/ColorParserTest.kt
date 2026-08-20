package com.carbroz.partner.core.ui.color

import androidx.compose.ui.graphics.Color
import kotlin.test.Test
import kotlin.test.assertEquals

class ColorParserTest {

    @Test
    fun testParseHexColor6Char() {
        val color = ColorParser.parseHexColor("#FF0000")
        assertEquals(Color(0xFFFF0000), color)
    }

    @Test
    fun testParseHexColor8Char() {
        val color = ColorParser.parseHexColor("#80FF0000")
        assertEquals(Color(0x80FF0000), color)
    }

    @Test
    fun testParseHexColorFallback() {
        val fallback = Color.Blue
        assertEquals(fallback, ColorParser.parseHexColor("invalid", fallback))
        assertEquals(fallback, ColorParser.parseHexColor(null, fallback))
    }
}
