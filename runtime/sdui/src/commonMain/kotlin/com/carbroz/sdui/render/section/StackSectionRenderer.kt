package com.carbroz.sdui.render.section

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.carbroz.sdui.model.SduiSection
import com.carbroz.sdui.registry.SectionRenderer
import com.carbroz.sdui.render.SduiRenderContext
import com.carbroz.sdui.render.layout.RenderChildLayout
import com.carbroz.sdui.render.modifier.applySduiProperties

object StackSectionRenderer : SectionRenderer {
    override val type: String = "stack_section"

    @Composable
    override fun Render(
        node: SduiSection,
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
