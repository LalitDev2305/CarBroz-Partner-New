package com.carbroz.runtime.sdui.template

import com.carbroz.runtime.sdui.extension.SduiDefinition
import com.carbroz.runtime.sdui.template.form.FormTemplateDefinition

object TemplateDefinitions {
    val all: List<SduiDefinition<*>> = listOf(FormTemplateDefinition)
}
