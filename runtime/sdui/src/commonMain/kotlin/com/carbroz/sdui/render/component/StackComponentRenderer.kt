package com.carbroz.sdui.render.component

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.carbroz.sdui.model.SduiComponent
import com.carbroz.sdui.registry.ComponentRenderer
import com.carbroz.sdui.render.SduiRenderContext
import com.carbroz.sdui.render.layout.RenderChildLayout
import com.carbroz.sdui.render.modifier.applySduiProperties

object StackComponentRenderer : ComponentRenderer {
    override val type: String = "stack_component"

    @Composable
    override fun Render(
        node: SduiComponent,
        context: SduiRenderContext,
        children: @Composable () -> Unit,
    ) {
        RenderChildLayout(
            properties = node.properties,
            modifier = Modifier.applySduiProperties(node.properties),
            children = children,
        )
    }
}
