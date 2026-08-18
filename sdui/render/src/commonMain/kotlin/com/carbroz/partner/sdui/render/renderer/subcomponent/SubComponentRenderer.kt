package com.carbroz.partner.sdui.render.renderer.subcomponent

import androidx.compose.runtime.Composable
import com.carbroz.partner.sdui.engine.model.SduiSubComponent
import com.carbroz.partner.sdui.render.scope.RenderScope

public fun interface SubComponentRenderer {
    @Composable
    public fun render(subComponent: SduiSubComponent, scope: RenderScope)
}
