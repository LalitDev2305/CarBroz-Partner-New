package com.carbroz.sdui.registry

import com.carbroz.sdui.render.element.ButtonElementRenderer
import com.carbroz.sdui.render.element.ImageElementRenderer
import com.carbroz.sdui.render.element.InputElementRenderer
import com.carbroz.sdui.render.element.TextElementRenderer
import com.carbroz.sdui.render.structuralComponentRenderer
import com.carbroz.sdui.render.structuralGroupRenderer
import com.carbroz.sdui.render.structuralSectionRenderer
import com.carbroz.sdui.render.structuralTemplateRenderer

object TemplateDefinitions {
    val all: List<TemplateRenderer> = listOf(
        structuralTemplateRenderer("stack_template"),
        structuralTemplateRenderer("form_template"),
        structuralTemplateRenderer("default_template"),
    )
}

object ComponentDefinitions {
    val all: List<ComponentRenderer> = listOf(
        structuralComponentRenderer("stack_component"),
    )
}

object SectionDefinitions {
    val all: List<SectionRenderer> = listOf(
        structuralSectionRenderer("stack_section"),
    )
}

object GroupDefinitions {
    val all: List<GroupRenderer> = listOf(
        structuralGroupRenderer("stack_group"),
    )
}

object ElementDefinitions {
    val all: List<ElementRenderer> = listOf(
        TextElementRenderer,
        ImageElementRenderer,
        InputElementRenderer,
        ButtonElementRenderer,
    )
}
