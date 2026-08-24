package com.carbroz.runtime.sdui.rendering

import androidx.compose.runtime.Composable
import com.carbroz.runtime.sdui.model.Component
import com.carbroz.runtime.sdui.model.ComponentContent
import com.carbroz.runtime.sdui.model.Element
import com.carbroz.runtime.sdui.model.Group
import com.carbroz.runtime.sdui.model.NodeKind
import com.carbroz.runtime.sdui.model.NodePath
import com.carbroz.runtime.sdui.model.NodeProperties
import com.carbroz.runtime.sdui.model.NodeType
import com.carbroz.runtime.sdui.model.Screen
import com.carbroz.runtime.sdui.model.Section
import com.carbroz.runtime.sdui.model.SectionContent
import com.carbroz.runtime.sdui.model.Template

/**
 * UI-only event emitted by SDUI renderers. It intentionally carries no execution behavior.
 * The owning MVI/runtime layer decides what to do with the event and the command attached to the node.
 */
sealed interface SduiRenderEvent {
    val path: NodePath

    data class Activated(override val path: NodePath) : SduiRenderEvent
    data class ValueChanged(override val path: NodePath, val value: String) : SduiRenderEvent
}

fun interface SduiEventSink {
    fun emit(event: SduiRenderEvent)
}

data class SduiRenderContext(
    val events: SduiEventSink,
)

/** Compose renderer owned by the same atomic definition that owns property decoding. */
interface SduiRenderer<P : NodeProperties, N : Any> {
    @Composable
    fun Render(node: N, properties: P, context: SduiRenderContext, children: @Composable () -> Unit)
}

interface RenderableSduiDefinition<P : NodeProperties, N : Any> {
    val renderer: SduiRenderer<P, N>
}

sealed interface SduiRenderFailure {
    data class MissingDefinition(val kind: NodeKind, val type: NodeType, val path: NodePath) : SduiRenderFailure
    data class DefinitionIsNotRenderable(val kind: NodeKind, val type: NodeType, val path: NodePath) : SduiRenderFailure
}

/**
 * Central traversal/dispatch boundary. Structural definitions decide layout; traversal remains centralized.
 * No renderer performs networking, navigation, command execution, or business work.
 */
class SduiRendererDispatcher(
    private val lookup: (NodeKind, NodeType) -> Any?,
) {
    @Composable
    fun RenderScreen(screen: Screen, context: SduiRenderContext, onFailure: (SduiRenderFailure) -> Unit) {
        renderTemplate(screen.template, context, onFailure)
    }

    @Composable
    private fun renderTemplate(node: Template, context: SduiRenderContext, onFailure: (SduiRenderFailure) -> Unit) {
        render(NodeKind.TEMPLATE, node.type, node.path, node.properties, node, context, onFailure) {
            node.components.forEach { renderComponent(it, context, onFailure) }
        }
    }

    @Composable
    private fun renderComponent(node: Component, context: SduiRenderContext, onFailure: (SduiRenderFailure) -> Unit) {
        render(NodeKind.COMPONENT, node.type, node.path, node.properties, node, context, onFailure) {
            when (val content = node.content) {
                is ComponentContent.Sections -> content.values.forEach { renderSection(it, context, onFailure) }
                is ComponentContent.Elements -> content.values.forEach { renderElement(it, context, onFailure) }
            }
        }
    }

    @Composable
    private fun renderSection(node: Section, context: SduiRenderContext, onFailure: (SduiRenderFailure) -> Unit) {
        render(NodeKind.SECTION, node.type, node.path, node.properties, node, context, onFailure) {
            when (val content = node.content) {
                is SectionContent.Groups -> content.values.forEach { renderGroup(it, context, onFailure) }
                is SectionContent.Elements -> content.values.forEach { renderElement(it, context, onFailure) }
            }
        }
    }

    @Composable
    private fun renderGroup(node: Group, context: SduiRenderContext, onFailure: (SduiRenderFailure) -> Unit) {
        render(NodeKind.GROUP, node.type, node.path, node.properties, node, context, onFailure) {
            node.elements.forEach { renderElement(it, context, onFailure) }
        }
    }

    @Composable
    private fun renderElement(node: Element, context: SduiRenderContext, onFailure: (SduiRenderFailure) -> Unit) {
        render(NodeKind.ELEMENT, node.type, node.path, node.properties, node, context, onFailure) {}
    }

    @Composable
    @Suppress("UNCHECKED_CAST")
    private fun <P : NodeProperties, N : Any> render(
        kind: NodeKind,
        type: NodeType,
        path: NodePath,
        properties: P,
        node: N,
        context: SduiRenderContext,
        onFailure: (SduiRenderFailure) -> Unit,
        children: @Composable () -> Unit,
    ) {
        val definition = lookup(kind, type)
        if (definition == null) {
            onFailure(SduiRenderFailure.MissingDefinition(kind, type, path))
            return
        }
        val renderable = definition as? RenderableSduiDefinition<P, N>
        if (renderable == null) {
            onFailure(SduiRenderFailure.DefinitionIsNotRenderable(kind, type, path))
            return
        }
        renderable.renderer.Render(node, properties, context, children)
    }
}
