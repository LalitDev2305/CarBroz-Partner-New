package com.carbroz.partner.sdui.render.scope

import androidx.compose.runtime.Composable
import com.carbroz.partner.core.ui.adaptive.context.ResolutionContext
import com.carbroz.partner.sdui.engine.model.SduiChild
import com.carbroz.partner.sdui.engine.model.SduiChildrenData
import com.carbroz.partner.sdui.engine.model.SduiComponent
import com.carbroz.partner.sdui.engine.model.SduiSubComponent
import com.carbroz.partner.sdui.engine.model.SduiTemplate
import com.carbroz.partner.sdui.render.runtime.event.SduiUiEventSink
import com.carbroz.partner.sdui.render.runtime.snapshot.SduiRenderSnapshot

public interface RenderScope {
    public val resolutionContext: ResolutionContext
    public val snapshot: SduiRenderSnapshot
    public val eventSink: SduiUiEventSink
    @Composable public fun renderTemplate(template: SduiTemplate)
    @Composable public fun renderComponent(component: SduiComponent)
    @Composable public fun renderSubComponent(subComponent: SduiSubComponent)
    @Composable public fun renderChild(child: SduiChild)
    @Composable public fun renderChildrenData(childrenData: SduiChildrenData)
}
