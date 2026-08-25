package com.carbroz.runtime.sdui.model

/** Closed structural positions in the SDUI protocol. Concrete server types remain open through [NodeType]. */
enum class NodeKind {
    TEMPLATE,
    COMPONENT,
    SECTION,
    GROUP,
    ELEMENT,
}

value class NodeType(val value: String)

value class NodeId(val value: String)

/** Canonical identity of a normalized node inside one screen tree. */
class NodePath private constructor(val segments: List<NodeId>) {
    init {
        require(segments.isNotEmpty()) { "NodePath cannot be empty" }
    }

    fun child(id: NodeId): NodePath = NodePath(segments + id)

    override fun equals(other: Any?): Boolean =
        this === other || (other is NodePath && segments == other.segments)

    override fun hashCode(): Int = segments.hashCode()

    override fun toString(): String = segments.joinToString("/") { it.value }

    companion object {
        fun root(id: NodeId): NodePath = NodePath(listOf(id))
    }
}

/** Marker implemented by typed, normalized definition-specific properties. */
interface NodeProperties

data object EmptyNodeProperties : NodeProperties

data class Screen(
    val id: NodeId,
    val version: Int,
    val template: Template,
)

data class Template(
    val id: NodeId,
    val path: NodePath,
    val type: NodeType,
    val properties: NodeProperties,
    val components: List<Component>,
)

data class Component(
    val id: NodeId,
    val path: NodePath,
    val type: NodeType,
    val properties: NodeProperties,
    val content: ComponentContent,
)

sealed interface ComponentContent {
    data class Sections(val values: List<Section>) : ComponentContent
    data class Elements(val values: List<Element>) : ComponentContent
}

data class Section(
    val id: NodeId,
    val path: NodePath,
    val type: NodeType,
    val properties: NodeProperties,
    val content: SectionContent,
)

sealed interface SectionContent {
    data class Groups(val values: List<Group>) : SectionContent
    data class Elements(val values: List<Element>) : SectionContent
}

data class Group(
    val id: NodeId,
    val path: NodePath,
    val type: NodeType,
    val properties: NodeProperties,
    val elements: List<Element>,
)

data class Element(
    val id: NodeId,
    val path: NodePath,
    val type: NodeType,
    val properties: NodeProperties,
    val command: Command?,
)
