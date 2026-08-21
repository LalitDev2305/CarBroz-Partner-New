package com.carbroz.foundation.localization

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class AppLocaleTest {
    @Test
    fun `language and alpha region normalize into language tag`() {
        assertEquals("en-IN", AppLocale("EN", "in").languageTag)
    }

    @Test
    fun `numeric region is preserved`() {
        assertEquals("es-419", AppLocale("es", "419").languageTag)
    }

    @Test
    fun `malformed locale components are rejected`() {
        assertFailsWith<IllegalArgumentException> { AppLocale("e") }
        assertFailsWith<IllegalArgumentException> { AppLocale("english") }
        assertFailsWith<IllegalArgumentException> { AppLocale("en", "IND") }
        assertFailsWith<IllegalArgumentException> { AppLocale("en", "1") }
    }
}
