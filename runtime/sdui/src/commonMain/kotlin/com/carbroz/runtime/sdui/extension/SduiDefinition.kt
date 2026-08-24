package com.carbroz.runtime.sdui.extension

import com.carbroz.runtime.sdui.model.NodeKind
import com.carbroz.runtime.sdui.model.NodeProperties
import com.carbroz.runtime.sdui.model.NodeType
import kotlinx.serialization.json.JsonObject

data class DefinitionKey(
    val kind: NodeKind,
    val type: NodeType,
)

sealed interface PropertyDecodeResult<out P : NodeProperties> {
    data class Success<P : NodeProperties>(val properties: P) : PropertyDecodeResult<P>
    data class Failure(val reason: String) : PropertyDecodeResult<Nothing>
}

/** Atomic extension contract. Rendering is added by the rendering layer without parallel registries. */
interface SduiDefinition<P : NodeProperties> {
    val kind: NodeKind
    val type: NodeType

    val key: DefinitionKey
        get() = DefinitionKey(kind, type)

    fun decodeProperties(raw: JsonObject): PropertyDecodeResult<P>
}

interface TemplateDefinition<P : NodeProperties> : SduiDefinition<P> {
    override val kind: NodeKind get() = NodeKind.TEMPLATE
}

interface ComponentDefinition<P : NodeProperties> : SduiDefinition<P> {
    override val kind: NodeKind get() = NodeKind.COMPONENT
}

interface SectionDefinition<P : NodeProperties> : SduiDefinition<P> {
    override val kind: NodeKind get() = NodeKind.SECTION
}

interface GroupDefinition<P : NodeProperties> : SduiDefinition<P> {
    override val kind: NodeKind get() = NodeKind.GROUP
}

interface ElementDefinition<P : NodeProperties> : SduiDefinition<P> {
    override val kind: NodeKind get() = NodeKind.ELEMENT
}
