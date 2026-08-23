package com.carbroz.foundation.localization

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class LocalizedTextTest {
    @Test
    fun `literal preserves already resolved content`() {
        assertEquals("CarBroz", LocalizedText.Literal("CarBroz").value)
    }

    @Test
    fun `key preserves arguments for presentation resolver`() {
        val text = LocalizedText.Key(
            key = "booking.status.confirmed",
            arguments = listOf("CB-123"),
        )

        assertEquals("booking.status.confirmed", text.key)
        assertEquals(listOf("CB-123"), text.arguments)
    }

    @Test
    fun `malformed localization keys are rejected`() {
        assertFailsWith<IllegalArgumentException> { LocalizedText.Key("A") }
        assertFailsWith<IllegalArgumentException> { LocalizedText.Key("invalid key") }
        assertFailsWith<IllegalArgumentException> { LocalizedText.Key("_hidden") }
    }
}
