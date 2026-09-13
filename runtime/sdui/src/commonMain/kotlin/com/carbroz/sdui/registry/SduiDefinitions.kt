package com.carbroz.sdui.registry

import com.carbroz.sdui.render.component.StackComponentRenderer
import com.carbroz.sdui.render.element.ButtonElementRenderer
import com.carbroz.sdui.render.element.ImageElementRenderer
import com.carbroz.sdui.render.element.InputElementRenderer
import com.carbroz.sdui.render.element.TextElementRenderer
import com.carbroz.sdui.render.group.StackGroupRenderer
import com.carbroz.sdui.render.section.StackSectionRenderer
import com.carbroz.sdui.render.template.DefaultTemplateRenderer
import com.carbroz.sdui.render.template.FormTemplateRenderer
import com.carbroz.sdui.render.template.StackTemplateRenderer

object TemplateDefinitions {
    val all: List<TemplateRenderer> = listOf(
        StackTemplateRenderer,
        FormTemplateRenderer,
        DefaultTemplateRenderer,
    )
}

object ComponentDefinitions {
    val all: List<ComponentRenderer> = listOf(
        StackComponentRenderer,
    )
}

object SectionDefinitions {
    val all: List<SectionRenderer> = listOf(
        StackSectionRenderer,
    )
}

object GroupDefinitions {
    val all: List<GroupRenderer> = listOf(
        StackGroupRenderer,
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
