package com.carbroz.runtime.sdui.element

import com.carbroz.runtime.sdui.element.button.ButtonElementDefinition
import com.carbroz.runtime.sdui.element.input.InputElementDefinition
import com.carbroz.runtime.sdui.element.text.TextElementDefinition
import com.carbroz.runtime.sdui.extension.SduiDefinition

object ElementDefinitions {
    val all: List<SduiDefinition<*>> = listOf(
        TextElementDefinition,
        ButtonElementDefinition,
        InputElementDefinition,
    )
}
