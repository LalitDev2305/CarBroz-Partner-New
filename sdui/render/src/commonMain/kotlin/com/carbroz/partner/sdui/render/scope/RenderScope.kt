package com.carbroz.partner.sdui.render.scope

import androidx.compose.runtime.Composable
import com.carbroz.partner.sdui.engine.model.SduiChild
import com.carbroz.partner.sdui.engine.model.SduiChildrenData
import com.carbroz.partner.sdui.engine.model.SduiComponent
import com.carbroz.partner.sdui.engine.model.SduiSubComponent
import com.carbroz.partner.sdui.engine.model.SduiTemplate

public interface RenderScope {
    @Composable public fun renderTemplate(template: SduiTemplate)
    @Composable public fun renderComponent(component: SduiComponent)
    @Composable public fun renderSubComponent(subComponent: SduiSubComponent)
    @Composable public fun renderChild(child: SduiChild)
    @Composable public fun renderChildrenData(childrenData: SduiChildrenData)
}
