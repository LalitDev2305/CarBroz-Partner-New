package com.carbroz.runtime.sdui.rendering

import androidx.compose.runtime.Composable
import com.carbroz.runtime.sdui.extension.SduiDefinition
import com.carbroz.runtime.sdui.model.Component
import com.carbroz.runtime.sdui.model.ComponentContent
import com.carbroz.runtime.sdui.model.Element
import com.carbroz.runtime.sdui.model.Group
import com.carbroz.runtime.sdui.model.NodeKind
import com.carbroz.runtime.sdui.model.NodePath
import com.carbroz.runtime.sdui.model.NodeType
import com.carbroz.runtime.sdui.model.Screen
import com.carbroz.runtime.sdui.model.Section
import com.carbroz.runtime.sdui.model.SectionContent
import com.carbroz.runtime.sdui.model.Template
import com.carbroz.runtime.sdui.registry.SduiRegistry

/** UI-only events. Command execution remains outside Compose rendering. */
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

/**
 * Multiplatform rendering contract exposed by an atomic SDUI definition.
 * Definitions opt into exactly the normalized node kinds they support; no reflection or unchecked generic
 * property cast is required by the dispatcher.
 */
interface RenderableSduiDefinition {
    @Composable
    fun RenderTemplate(node: Template, context: SduiRenderContext, children: @Composable () -> Unit): Boolean = false

    @Composable
    fun RenderComponent(node: Component, context: SduiRenderContext, children: @Composable () -> Unit): Boolean = false

    @Composable
    fun RenderSection(node: Section, context: SduiRenderContext, children: @Composable () -> Unit): Boolean = false

    @Composable
    fun RenderGroup(node: Group, context: SduiRenderContext, children: @Composable () -> Unit): Boolean = false

    @Composable
    fun RenderElement(node: Element, context: SduiRenderContext): Boolean = false
}

sealed interface SduiRenderFailure {
    data class MissingDefinition(val kind: NodeKind, val type: NodeType, val path: NodePath) : SduiRenderFailure
    data class DefinitionIsNotRenderable(val kind: NodeKind, val type: NodeType, val path: NodePath) : SduiRenderFailure
    data class DefinitionRejectedNode(val kind: NodeKind, val type: NodeType, val path: NodePath) : SduiRenderFailure
}

/** Central structural traversal and definition dispatch. */
class SduiRendererDispatcher(
    private val registry: SduiRegistry,
) {
    @Composable
    fun RenderScreen(screen: Screen, context: SduiRenderContext, onFailure: (SduiRenderFailure) -> Unit) {
        renderTemplate(screen.template, context, onFailure)
    }

    @Composable
    private fun renderTemplate(node: Template, context: SduiRenderContext, onFailure: (SduiRenderFailure) -> Unit) {
        dispatch(NodeKind.TEMPLATE, node.type, node.path, onFailure) { definition ->
            definition.RenderTemplate(node, context) {
                node.components.forEach { renderComponent(it, context, onFailure) }
            }
        }
    }

    @Composable
    private fun renderComponent(node: Component, context: SduiRenderContext, onFailure: (SduiRenderFailure) -> Unit) {
        dispatch(NodeKind.COMPONENT, node.type, node.path, onFailure) { definition ->
            definition.RenderComponent(node, context) {
                when (val content = node.content) {
                    is ComponentContent.Sections -> content.values.forEach { renderSection(it, context, onFailure) }
                    is ComponentContent.Elements -> content.values.forEach { renderElement(it, context, onFailure) }
                }
            }
        }
    }

    @Composable
    private fun renderSection(node: Section, context: SduiRenderContext, onFailure: (SduiRenderFailure) -> Unit) {
        dispatch(NodeKind.SECTION, node.type, node.path, onFailure) { definition ->
            definition.RenderSection(node, context) {
                when (val content = node.content) {
                    is SectionContent.Groups -> content.values.forEach { renderGroup(it, context, onFailure) }
                    is SectionContent.Elements -> content.values.forEach { renderElement(it, context, onFailure) }
                }
            }
        }
    }

    @Composable
    private fun renderGroup(node: Group, context: SduiRenderContext, onFailure: (SduiRenderFailure) -> Unit) {
        dispatch(NodeKind.GROUP, node.type, node.path, onFailure) { definition ->
            definition.RenderGroup(node, context) {
                node.elements.forEach { renderElement(it, context, onFailure) }
            }
        }
    }

    @Composable
    private fun renderElement(node: Element, context: SduiRenderContext, onFailure: (SduiRenderFailure) -> Unit) {
        dispatch(NodeKind.ELEMENT, node.type, node.path, onFailure) { definition ->
            definition.RenderElement(node, context)
        }
    }

    @Composable
    private fun dispatch(
        kind: NodeKind,
        type: NodeType,
        path: NodePath,
        onFailure: (SduiRenderFailure) -> Unit,
        render: @Composable (RenderableSduiDefinition) -> Boolean,
    ) {
        val registered: SduiDefinition<*> = registry.find(kind, type) ?: run {
            onFailure(SduiRenderFailure.MissingDefinition(kind, type, path))
            return
        }
        val definition = registered as? RenderableSduiDefinition ?: run {
            onFailure(SduiRenderFailure.DefinitionIsNotRenderable(kind, type, path))
            return
        }
        if (!render(definition)) {
            onFailure(SduiRenderFailure.DefinitionRejectedNode(kind, type, path))
        }
    }
}
