package com.carbroz.partner.sdui.render.renderer.child

import androidx.compose.runtime.Composable
import com.carbroz.partner.sdui.engine.model.SduiChild
import com.carbroz.partner.sdui.render.scope.RenderScope

public fun interface ChildRenderer {
    @Composable
    public fun render(child: SduiChild, scope: RenderScope)
}
