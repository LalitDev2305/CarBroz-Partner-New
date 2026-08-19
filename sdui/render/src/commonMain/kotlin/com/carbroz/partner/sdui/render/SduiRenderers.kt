package com.carbroz.partner.sdui.render

import com.carbroz.partner.sdui.render.registry.ChildRendererRegistry
import com.carbroz.partner.sdui.render.registry.ChildrenDataRendererRegistry
import com.carbroz.partner.sdui.render.registry.ComponentRendererRegistry
import com.carbroz.partner.sdui.render.registry.SubComponentRendererRegistry
import com.carbroz.partner.sdui.render.registry.TemplateRendererRegistry
import com.carbroz.partner.sdui.render.renderer.child.ChildRenderer
import com.carbroz.partner.sdui.render.renderer.child.ContainerChild
import com.carbroz.partner.sdui.render.renderer.childdata.ChildrenDataRenderer
import com.carbroz.partner.sdui.render.renderer.childdata.button.ButtonChildrenData
import com.carbroz.partner.sdui.render.renderer.childdata.input.InputChildrenData
import com.carbroz.partner.sdui.render.renderer.childdata.text.TextChildrenData
import com.carbroz.partner.sdui.render.renderer.childdata.timer.TimerChildrenData
import com.carbroz.partner.sdui.render.renderer.component.ComponentRenderer
import com.carbroz.partner.sdui.render.renderer.component.ContainerComponent
import com.carbroz.partner.sdui.render.renderer.subcomponent.ContainerSubComponent
import com.carbroz.partner.sdui.render.renderer.subcomponent.SubComponentRenderer
import com.carbroz.partner.sdui.render.renderer.template.ContainerTemplate
import com.carbroz.partner.sdui.render.renderer.template.FormTemplate
import com.carbroz.partner.sdui.render.renderer.template.TemplateRenderer

public object SduiRenderers {

    public val defaultTemplates: TemplateRendererRegistry = TemplateRendererRegistry(
        mapOf(
            "container_template" to (ContainerTemplate as TemplateRenderer),
            "form_template" to (FormTemplate as TemplateRenderer)
        )
    )

    public val defaultComponents: ComponentRendererRegistry = ComponentRendererRegistry(
        mapOf(
            "container_component" to (ContainerComponent as ComponentRenderer)
        )
    )

    public val defaultSubComponents: SubComponentRendererRegistry = SubComponentRendererRegistry(
        mapOf(
            "container_subcomponent" to (ContainerSubComponent as SubComponentRenderer)
        )
    )

    public val defaultChildren: ChildRendererRegistry = ChildRendererRegistry(
        mapOf(
            "container_child" to (ContainerChild as ChildRenderer)
        )
    )

    public val defaultChildrenData: ChildrenDataRendererRegistry = ChildrenDataRendererRegistry(
        mapOf(
            "text" to (TextChildrenData as ChildrenDataRenderer),
            "button" to (ButtonChildrenData as ChildrenDataRenderer),
            "input" to (InputChildrenData as ChildrenDataRenderer),
            "timer" to (TimerChildrenData as ChildrenDataRenderer)
        )
    )
}
