package com.carbroz.runtime.sdui.template

import com.carbroz.runtime.sdui.component.Component
import com.carbroz.runtime.sdui.model.NodeId
import com.carbroz.runtime.sdui.model.NodePath
import com.carbroz.runtime.sdui.model.NodeProperties
import com.carbroz.runtime.sdui.model.NodeType

data class Template(
    val id: NodeId,
    val path: NodePath,
    val type: NodeType,
    val properties: NodeProperties,
    val components: List<Component>,
)
