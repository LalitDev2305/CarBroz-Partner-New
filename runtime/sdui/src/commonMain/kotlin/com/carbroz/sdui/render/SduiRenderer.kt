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

    /** Renders a backend presentation target with the same hierarchy renderers used by the base screen. */
    @Composable
    fun RenderTarget(screen: SduiScreen, targetId: String, context: SduiRenderContext) {
        screen.template.components.forEach { component ->
            if (component.id == targetId) {
                RenderComponent(component, context)
                return
            }
            component.elements.orEmpty().firstOrNull { it.id == targetId }?.let {
                RenderElement(it, context)
                return
            }
            component.sections.orEmpty().forEach { section ->
                if (section.id == targetId) {
                    RenderSection(section, context)
                    return
                }
                section.elements.orEmpty().firstOrNull { it.id == targetId }?.let {
                    RenderElement(it, context)
                    return
                }
                section.groups.orEmpty().forEach { group ->
                    if (group.id == targetId) {
                        RenderGroup(group, context)
                        return
                    }
                    group.elements.firstOrNull { it.id == targetId }?.let {
                        RenderElement(it, context)
                        return
                    }
                }
            }
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
