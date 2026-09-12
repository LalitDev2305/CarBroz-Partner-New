package com.carbroz.sdui.render.group

import androidx.compose.runtime.Composable
import com.carbroz.sdui.model.SduiGroup
import com.carbroz.sdui.registry.GroupRenderer
import com.carbroz.sdui.render.RenderStack
import com.carbroz.sdui.render.SduiRenderContext

object StackGroupRenderer : GroupRenderer {
    override val type: String = "stack_group"

    @Composable
    override fun Render(node: SduiGroup, context: SduiRenderContext, children: @Composable () -> Unit) {
        RenderStack(node.properties, children = children)
    }
}
