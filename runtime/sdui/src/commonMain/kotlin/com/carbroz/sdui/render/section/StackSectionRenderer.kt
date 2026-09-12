package com.carbroz.sdui.render.section

import androidx.compose.runtime.Composable
import com.carbroz.sdui.model.SduiSection
import com.carbroz.sdui.registry.SectionRenderer
import com.carbroz.sdui.render.RenderStack
import com.carbroz.sdui.render.SduiRenderContext

object StackSectionRenderer : SectionRenderer {
    override val type: String = "stack_section"

    @Composable
    override fun Render(node: SduiSection, context: SduiRenderContext, children: @Composable () -> Unit) {
        RenderStack(node.properties, children = children)
    }
}
