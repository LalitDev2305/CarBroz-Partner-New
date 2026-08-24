package com.carbroz.runtime.sdui.component

import com.carbroz.runtime.sdui.element.Element
import com.carbroz.runtime.sdui.model.NodeId
import com.carbroz.runtime.sdui.model.NodePath
import com.carbroz.runtime.sdui.model.NodeProperties
import com.carbroz.runtime.sdui.model.NodeType
import com.carbroz.runtime.sdui.section.Section

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
