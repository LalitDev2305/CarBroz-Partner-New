package com.carbroz.sdui.registry

import com.carbroz.sdui.render.component.StackComponentRenderer
import com.carbroz.sdui.render.element.ButtonElementRenderer
import com.carbroz.sdui.render.element.ImageElementRenderer
import com.carbroz.sdui.render.element.InputElementRenderer
import com.carbroz.sdui.render.element.TextElementRenderer
import com.carbroz.sdui.render.group.StackGroupRenderer
import com.carbroz.sdui.render.section.StackSectionRenderer
import com.carbroz.sdui.render.template.DefaultTemplateRenderer
import com.carbroz.sdui.render.template.FormTemplateRenderer
import com.carbroz.sdui.render.template.StackTemplateRenderer
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
    fun lookup_returnsExactConcreteRendererForEveryCurrentWireType() {
        val registry = SduiNodeRegistration.createRegistry()

        assertSame(FormTemplateRenderer, registry.template("form_template"))
        assertSame(StackTemplateRenderer, registry.template("stack_template"))
        assertSame(DefaultTemplateRenderer, registry.template("default_template"))
        assertSame(StackComponentRenderer, registry.component("stack_component"))
        assertSame(StackSectionRenderer, registry.section("stack_section"))
        assertSame(StackGroupRenderer, registry.group("stack_group"))
        assertSame(TextElementRenderer, registry.element("text"))
        assertSame(ImageElementRenderer, registry.element("image"))
        assertSame(InputElementRenderer, registry.element("input"))
        assertSame(ButtonElementRenderer, registry.element("button"))
    }

    @Test
    fun definitionsExposeTheConcreteRendererObjects() {
        assertSame(StackTemplateRenderer, TemplateDefinitions.all.single { it.type == "stack_template" })
        assertSame(FormTemplateRenderer, TemplateDefinitions.all.single { it.type == "form_template" })
        assertSame(DefaultTemplateRenderer, TemplateDefinitions.all.single { it.type == "default_template" })
        assertSame(StackComponentRenderer, ComponentDefinitions.all.single { it.type == "stack_component" })
        assertSame(StackSectionRenderer, SectionDefinitions.all.single { it.type == "stack_section" })
        assertSame(StackGroupRenderer, GroupDefinitions.all.single { it.type == "stack_group" })
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
