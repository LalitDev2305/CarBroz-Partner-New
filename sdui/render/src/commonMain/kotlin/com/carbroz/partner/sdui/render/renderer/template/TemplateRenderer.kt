package com.carbroz.partner.sdui.render.renderer.template

import androidx.compose.runtime.Composable
import com.carbroz.partner.sdui.engine.model.SduiTemplate
import com.carbroz.partner.sdui.render.scope.RenderScope

public fun interface TemplateRenderer {
    @Composable
    public fun render(template: SduiTemplate, scope: RenderScope)
}
