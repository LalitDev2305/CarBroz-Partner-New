package com.carbroz.runtime.sdui.component.stack

import androidx.compose.runtime.Composable
import com.carbroz.runtime.sdui.extension.ComponentDefinition
import com.carbroz.runtime.sdui.extension.PropertyDecodeResult
import com.carbroz.runtime.sdui.model.Component
import com.carbroz.runtime.sdui.model.NodeType
import com.carbroz.runtime.sdui.properties.StackContainerProperties
import com.carbroz.runtime.sdui.properties.StackContainerPropertiesDecoder
import com.carbroz.runtime.sdui.rendering.RenderStackContainer
import com.carbroz.runtime.sdui.rendering.RenderableSduiDefinition
import com.carbroz.runtime.sdui.rendering.SduiRenderContext
import kotlinx.serialization.json.JsonObject

object StackComponentDefinition : ComponentDefinition<StackContainerProperties>, RenderableSduiDefinition {
    override val type: NodeType = NodeType("STACK")
    override fun decodeProperties(raw: JsonObject): PropertyDecodeResult<StackContainerProperties> =
        StackContainerPropertiesDecoder.decode(raw)

    @Composable
    override fun RenderComponent(node: Component, context: SduiRenderContext, children: @Composable () -> Unit): Boolean {
        val properties = node.properties as? StackContainerProperties ?: return false
        RenderStackContainer(properties, children)
        return true
    }
}
