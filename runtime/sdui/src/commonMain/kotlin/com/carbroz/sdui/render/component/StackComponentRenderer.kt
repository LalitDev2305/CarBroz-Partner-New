package com.carbroz.sdui.render.component

import androidx.compose.runtime.Composable
import com.carbroz.sdui.model.SduiComponent
import com.carbroz.sdui.registry.ComponentRenderer
import com.carbroz.sdui.render.RenderStack
import com.carbroz.sdui.render.SduiRenderContext

object StackComponentRenderer : ComponentRenderer {
    override val type: String = "stack_component"

    @Composable
    override fun Render(node: SduiComponent, context: SduiRenderContext, children: @Composable () -> Unit) {
        RenderStack(node.properties, children = children)
    }
}
