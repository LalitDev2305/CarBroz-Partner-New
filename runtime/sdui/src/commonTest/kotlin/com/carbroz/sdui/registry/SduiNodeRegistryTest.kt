package com.carbroz.sdui.registry

import com.carbroz.sdui.render.element.TextElementRenderer
import com.carbroz.sdui.render.template.FormTemplateRenderer
import kotlin.test.Test
import kotlin.test.assertFailsWith
import kotlin.test.assertSame
import kotlin.test.assertTrue

class SduiNodeRegistryTest {
    @Test
    fun createRegistry_registersEveryFrozenHierarchy() {
        val registry = SduiNodeRegistration.createRegistry()

        assertTrue(registry.supportsTemplate("form_template"))
        assertTrue(registry.supportsTemplate("stack_template"))
        assertTrue(registry.supportsTemplate("default_template"))
        assertTrue(registry.supportsComponent("stack_component"))
        assertTrue(registry.supportsSection("stack_section"))
        assertTrue(registry.supportsGroup("stack_group"))
        assertTrue(registry.supportsElement("text"))
        assertTrue(registry.supportsElement("image"))
        assertTrue(registry.supportsElement("input"))
        assertTrue(registry.supportsElement("button"))
    }

    @Test
    fun lookup_returnsTheRegisteredRenderer() {
        val registry = SduiNodeRegistration.createRegistry()

        assertSame(FormTemplateRenderer, registry.template("form_template"))
        assertSame(TextElementRenderer, registry.element("text"))
    }

    @Test
    fun duplicateTemplateRegistration_failsFast() {
        val registry = SduiNodeRegistry()
        registry.registerTemplates(listOf(FormTemplateRenderer))

        assertFailsWith<IllegalArgumentException> {
            registry.registerTemplates(listOf(FormTemplateRenderer))
        }
    }

    @Test
    fun duplicateElementRegistration_failsFast() {
        val registry = SduiNodeRegistry()
        registry.registerElements(listOf(TextElementRenderer))

        assertFailsWith<IllegalArgumentException> {
            registry.registerElements(listOf(TextElementRenderer))
        }
    }
}
