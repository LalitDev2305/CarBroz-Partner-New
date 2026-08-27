package com.carbroz.runtime.sdui.registry

import com.carbroz.runtime.sdui.component.ComponentDefinitions
import com.carbroz.runtime.sdui.element.ElementDefinitions
import com.carbroz.runtime.sdui.group.GroupDefinitions
import com.carbroz.runtime.sdui.section.SectionDefinitions
import com.carbroz.runtime.sdui.template.TemplateDefinitions

/** Composite wiring only; concrete definitions remain owned by their hierarchy level. */
object SduiRegistryFactory {
    fun createCore(): SduiRegistry = SduiRegistryBuilder().apply {
        registerAll(TemplateDefinitions.all)
        registerAll(ComponentDefinitions.all)
        registerAll(SectionDefinitions.all)
        registerAll(GroupDefinitions.all)
        registerAll(ElementDefinitions.all)
    }.build()
}
