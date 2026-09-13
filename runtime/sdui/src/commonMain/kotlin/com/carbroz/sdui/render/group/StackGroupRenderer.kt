package com.carbroz.sdui.render.group

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.carbroz.sdui.model.SduiGroup
import com.carbroz.sdui.registry.GroupRenderer
import com.carbroz.sdui.render.SduiRenderContext
import com.carbroz.sdui.render.layout.RenderChildLayout
import com.carbroz.sdui.render.modifier.applySduiProperties

object StackGroupRenderer : GroupRenderer {
    override val type: String = "stack_group"

    @Composable
    override fun Render(
        node: SduiGroup,
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
