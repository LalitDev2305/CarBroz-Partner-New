package com.carbroz.partner.sdui.render.registry

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class RendererRegistryTest {

    @Test
    fun testNormalizedKeyLookupAndCaseInsensitivity() {
        val registry = RendererRegistry(
            mapOf("  Form_Template  " to "FormRendererValue")
        )

        assertEquals("FormRendererValue", registry.resolve("form_template"))
        assertEquals("FormRendererValue", registry.resolve("FORM_TEMPLATE"))
        assertEquals("FormRendererValue", registry.resolve("  form_template  "))
        assertTrue(registry.contains("form_template"))
    }

    @Test
    fun testDuplicateNormalizedKeyFailsFast() {
        val error = assertFailsWith<IllegalArgumentException> {
            RendererRegistry(
                mapOf(
                    "form" to "Value1",
                    "FORM" to "Value2"
                )
            )
        }
        assertTrue(error.message!!.contains("Duplicate normalized registry key"))
    }

    @Test
    fun testUnknownTypeReturnsNull() {
        val registry = RendererRegistry(mapOf("button" to "BtnRenderer"))
        assertNull(registry.resolve("unknown_type"))
        assertFalse(registry.contains("unknown_type"))
    }
}
