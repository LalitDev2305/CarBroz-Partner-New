package com.carbroz.sdui.render.template

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.carbroz.sdui.model.SduiTemplate
import com.carbroz.sdui.registry.TemplateRenderer
import com.carbroz.sdui.render.SduiRenderContext
import com.carbroz.sdui.render.layout.RenderChildLayout
import com.carbroz.sdui.render.modifier.applySduiProperties

object DefaultTemplateRenderer : TemplateRenderer {
    override val type: String = "default_template"

    @Composable
    override fun Render(
        node: SduiTemplate,
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
