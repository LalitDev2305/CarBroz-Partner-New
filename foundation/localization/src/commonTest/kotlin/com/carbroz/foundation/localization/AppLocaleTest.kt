package com.carbroz.foundation.localization

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class AppLocaleTest {
    @Test
    fun `language and alpha region normalize into language tag`() {
        assertEquals("en-IN", AppLocale(language = "EN", region = "in").languageTag)
    }

    @Test
    fun `script and region normalize into canonical language tag`() {
        assertEquals(
            "zh-Hant-TW",
            AppLocale(language = "ZH", script = "hANT", region = "tw").languageTag,
        )
    }

    @Test
    fun `numeric region is preserved`() {
        assertEquals("es-419", AppLocale(language = "es", region = "419").languageTag)
    }

    @Test
    fun `malformed locale components are rejected`() {
        assertFailsWith<IllegalArgumentException> { AppLocale(language = "e") }
        assertFailsWith<IllegalArgumentException> { AppLocale(language = "english") }
        assertFailsWith<IllegalArgumentException> { AppLocale(language = "en", script = "Lat") }
        assertFailsWith<IllegalArgumentException> { AppLocale(language = "en", script = "Latin") }
        assertFailsWith<IllegalArgumentException> { AppLocale(language = "en", region = "IND") }
        assertFailsWith<IllegalArgumentException> { AppLocale(language = "en", region = "1") }
    }
}
