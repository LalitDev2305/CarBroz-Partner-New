package com.carbroz.sdui.render

import androidx.compose.runtime.Composable
import com.carbroz.sdui.model.SduiComponent
import com.carbroz.sdui.model.SduiElement
import com.carbroz.sdui.model.SduiGroup
import com.carbroz.sdui.model.SduiScreen
import com.carbroz.sdui.model.SduiSection
import com.carbroz.sdui.registry.SduiNodeRegistry

class SduiRenderer(
    private val registry: SduiNodeRegistry,
) {
    @Composable
    fun Render(screen: SduiScreen, context: SduiRenderContext) {
        val template = screen.template
        val renderer = registry.template(template.type) ?: return
        renderer.Render(template, context) {
            template.components.forEach { component -> RenderComponent(component, context) }
        }
    }

    @Composable
    private fun RenderComponent(node: SduiComponent, context: SduiRenderContext) {
        val renderer = registry.component(node.type) ?: return
        renderer.Render(node, context) {
            node.elements?.forEach { RenderElement(it, context) }
            node.sections?.forEach { RenderSection(it, context) }
        }
    }

    @Composable
    private fun RenderSection(node: SduiSection, context: SduiRenderContext) {
        val renderer = registry.section(node.type) ?: return
        renderer.Render(node, context) {
            node.elements?.forEach { RenderElement(it, context) }
            node.groups?.forEach { RenderGroup(it, context) }
        }
    }

    @Composable
    private fun RenderGroup(node: SduiGroup, context: SduiRenderContext) {
        val renderer = registry.group(node.type) ?: return
        renderer.Render(node, context) {
            node.elements.forEach { RenderElement(it, context) }
        }
    }

    @Composable
    private fun RenderElement(node: SduiElement, context: SduiRenderContext) {
        registry.element(node.type)?.Render(node, context)
    }
}
