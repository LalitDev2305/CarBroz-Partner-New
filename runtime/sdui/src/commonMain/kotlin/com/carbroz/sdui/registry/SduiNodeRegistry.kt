package com.carbroz.sdui.registry

import androidx.compose.runtime.Composable
import com.carbroz.sdui.model.SduiComponent
import com.carbroz.sdui.model.SduiElement
import com.carbroz.sdui.model.SduiGroup
import com.carbroz.sdui.model.SduiSection
import com.carbroz.sdui.model.SduiTemplate
import com.carbroz.sdui.render.SduiRenderContext

interface TemplateRenderer {
    val type: String
    @Composable fun Render(node: SduiTemplate, context: SduiRenderContext, children: @Composable () -> Unit)
}

interface ComponentRenderer {
    val type: String
    @Composable fun Render(node: SduiComponent, context: SduiRenderContext, children: @Composable () -> Unit)
}

interface SectionRenderer {
    val type: String
    @Composable fun Render(node: SduiSection, context: SduiRenderContext, children: @Composable () -> Unit)
}

interface GroupRenderer {
    val type: String
    @Composable fun Render(node: SduiGroup, context: SduiRenderContext, children: @Composable () -> Unit)
}

interface ElementRenderer {
    val type: String
    @Composable fun Render(node: SduiElement, context: SduiRenderContext)
}

class SduiNodeRegistry {
    private val templates = linkedMapOf<String, TemplateRenderer>()
    private val components = linkedMapOf<String, ComponentRenderer>()
    private val sections = linkedMapOf<String, SectionRenderer>()
    private val groups = linkedMapOf<String, GroupRenderer>()
    private val elements = linkedMapOf<String, ElementRenderer>()

    fun registerTemplates(values: Iterable<TemplateRenderer>) = values.forEach { register(templates, it.type, it, "template") }
    fun registerComponents(values: Iterable<ComponentRenderer>) = values.forEach { register(components, it.type, it, "component") }
    fun registerSections(values: Iterable<SectionRenderer>) = values.forEach { register(sections, it.type, it, "section") }
    fun registerGroups(values: Iterable<GroupRenderer>) = values.forEach { register(groups, it.type, it, "group") }
    fun registerElements(values: Iterable<ElementRenderer>) = values.forEach { register(elements, it.type, it, "element") }

    fun template(type: String): TemplateRenderer? = templates[type]
    fun component(type: String): ComponentRenderer? = components[type]
    fun section(type: String): SectionRenderer? = sections[type]
    fun group(type: String): GroupRenderer? = groups[type]
    fun element(type: String): ElementRenderer? = elements[type]

    fun supportsTemplate(type: String): Boolean = type in templates
    fun supportsComponent(type: String): Boolean = type in components
    fun supportsSection(type: String): Boolean = type in sections
    fun supportsGroup(type: String): Boolean = type in groups
    fun supportsElement(type: String): Boolean = type in elements

    private fun <T> register(target: MutableMap<String, T>, type: String, value: T, hierarchy: String) {
        require(type.isNotBlank()) { "SDUI $hierarchy renderer type must not be blank" }
        require(target.putIfAbsent(type, value) == null) { "Duplicate SDUI $hierarchy renderer '$type'" }
    }
}
