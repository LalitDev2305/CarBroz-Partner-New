package com.carbroz.runtime.sdui.group

import com.carbroz.runtime.sdui.element.Element
import com.carbroz.runtime.sdui.model.NodeId
import com.carbroz.runtime.sdui.model.NodePath
import com.carbroz.runtime.sdui.model.NodeProperties
import com.carbroz.runtime.sdui.model.NodeType

data class Group(
    val id: NodeId,
    val path: NodePath,
    val type: NodeType,
    val properties: NodeProperties,
    val elements: List<Element>,
)
