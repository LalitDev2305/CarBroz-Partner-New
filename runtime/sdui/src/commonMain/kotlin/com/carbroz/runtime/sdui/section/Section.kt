package com.carbroz.runtime.sdui.section

import com.carbroz.runtime.sdui.element.Element
import com.carbroz.runtime.sdui.group.Group
import com.carbroz.runtime.sdui.model.NodeId
import com.carbroz.runtime.sdui.model.NodePath
import com.carbroz.runtime.sdui.model.NodeProperties
import com.carbroz.runtime.sdui.model.NodeType

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
