package com.carbroz.runtime.sdui.component

import com.carbroz.runtime.sdui.component.stack.StackComponentDefinition
import com.carbroz.runtime.sdui.extension.SduiDefinition

object ComponentDefinitions {
    val all: List<SduiDefinition<*>> = listOf(StackComponentDefinition)
}
