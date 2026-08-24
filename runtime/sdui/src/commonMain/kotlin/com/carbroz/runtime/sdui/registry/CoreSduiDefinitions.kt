package com.carbroz.runtime.sdui.registry

import com.carbroz.runtime.sdui.component.stack.StackComponentDefinition
import com.carbroz.runtime.sdui.element.button.ButtonElementDefinition
import com.carbroz.runtime.sdui.element.text.TextElementDefinition
import com.carbroz.runtime.sdui.extension.SduiDefinition
import com.carbroz.runtime.sdui.group.stack.StackGroupDefinition
import com.carbroz.runtime.sdui.section.stack.StackSectionDefinition
import com.carbroz.runtime.sdui.template.form.FormTemplateDefinition

/**
 * Registration bundle only. The immutable registry remains the sole runtime lookup mechanism.
 * Applications may register this set and add product-specific definitions explicitly.
 */
object CoreSduiDefinitions {
    val all: List<SduiDefinition<*>> = listOf(
        FormTemplateDefinition,
        StackComponentDefinition,
        StackSectionDefinition,
        StackGroupDefinition,
        TextElementDefinition,
        ButtonElementDefinition,
    )
}
