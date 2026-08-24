package com.carbroz.runtime.sdui.element

import com.carbroz.runtime.sdui.model.Command
import com.carbroz.runtime.sdui.model.NodeId
import com.carbroz.runtime.sdui.model.NodePath
import com.carbroz.runtime.sdui.model.NodeProperties
import com.carbroz.runtime.sdui.model.NodeType

data class Element(
    val id: NodeId,
    val path: NodePath,
    val type: NodeType,
    val properties: NodeProperties,
    val command: Command?,
)
