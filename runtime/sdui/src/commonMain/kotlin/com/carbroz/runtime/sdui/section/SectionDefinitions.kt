package com.carbroz.runtime.sdui.section

import com.carbroz.runtime.sdui.extension.SduiDefinition
import com.carbroz.runtime.sdui.section.stack.StackSectionDefinition

object SectionDefinitions {
    val all: List<SduiDefinition<*>> = listOf(StackSectionDefinition)
}
