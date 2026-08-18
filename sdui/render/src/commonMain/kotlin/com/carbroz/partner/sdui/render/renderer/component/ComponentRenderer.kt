package com.carbroz.partner.sdui.render.renderer.component

import androidx.compose.runtime.Composable
import com.carbroz.partner.sdui.engine.model.SduiComponent
import com.carbroz.partner.sdui.render.scope.RenderScope

public fun interface ComponentRenderer {
    @Composable
    public fun render(component: SduiComponent, scope: RenderScope)
}
