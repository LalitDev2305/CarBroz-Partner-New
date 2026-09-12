package com.carbroz.sdui.render.template

import androidx.compose.runtime.Composable
import com.carbroz.sdui.model.SduiTemplate
import com.carbroz.sdui.registry.TemplateRenderer
import com.carbroz.sdui.render.RenderStack
import com.carbroz.sdui.render.SduiRenderContext

object DefaultTemplateRenderer : TemplateRenderer {
    override val type: String = "default_template"

    @Composable
    override fun Render(node: SduiTemplate, context: SduiRenderContext, children: @Composable () -> Unit) {
        RenderStack(node.properties, children = children)
    }
}
