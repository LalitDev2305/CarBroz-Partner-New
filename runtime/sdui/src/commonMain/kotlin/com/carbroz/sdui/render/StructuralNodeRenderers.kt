package com.carbroz.sdui.render

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.carbroz.sdui.model.SduiComponent
import com.carbroz.sdui.model.SduiGroup
import com.carbroz.sdui.model.SduiSection
import com.carbroz.sdui.model.SduiTemplate
import com.carbroz.sdui.registry.ComponentRenderer
import com.carbroz.sdui.registry.GroupRenderer
import com.carbroz.sdui.registry.SectionRenderer
import com.carbroz.sdui.registry.TemplateRenderer
import com.carbroz.sdui.render.layout.RenderChildLayout
import com.carbroz.sdui.render.modifier.applySduiProperties
import kotlinx.serialization.json.JsonObject

internal fun structuralTemplateRenderer(wireType: String): TemplateRenderer = object : TemplateRenderer {
    override val type: String = wireType

    @Composable
    override fun Render(node: SduiTemplate, context: SduiRenderContext, children: @Composable () -> Unit) {
        RenderStructuralNode(node.properties, children)
    }
}

internal fun structuralComponentRenderer(wireType: String): ComponentRenderer = object : ComponentRenderer {
    override val type: String = wireType

    @Composable
    override fun Render(node: SduiComponent, context: SduiRenderContext, children: @Composable () -> Unit) {
        RenderStructuralNode(node.properties, children)
    }
}

internal fun structuralSectionRenderer(wireType: String): SectionRenderer = object : SectionRenderer {
    override val type: String = wireType

    @Composable
    override fun Render(node: SduiSection, context: SduiRenderContext, children: @Composable () -> Unit) {
        RenderStructuralNode(node.properties, children)
    }
}

internal fun structuralGroupRenderer(wireType: String): GroupRenderer = object : GroupRenderer {
    override val type: String = wireType

    @Composable
    override fun Render(node: SduiGroup, context: SduiRenderContext, children: @Composable () -> Unit) {
        RenderStructuralNode(node.properties, children)
    }
}

@Composable
private fun RenderStructuralNode(
    properties: JsonObject,
    children: @Composable () -> Unit,
) {
    RenderChildLayout(
        properties = properties,
        modifier = Modifier.applySduiProperties(properties),
        children = children,
    )
}
