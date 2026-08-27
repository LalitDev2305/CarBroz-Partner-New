package com.carbroz.runtime.sdui.section.stack

import androidx.compose.runtime.Composable
import com.carbroz.runtime.sdui.extension.PropertyDecodeResult
import com.carbroz.runtime.sdui.extension.SectionDefinition
import com.carbroz.runtime.sdui.model.NodeType
import com.carbroz.runtime.sdui.model.Section
import com.carbroz.runtime.sdui.properties.StackContainerProperties
import com.carbroz.runtime.sdui.properties.StackContainerPropertiesDecoder
import com.carbroz.runtime.sdui.rendering.RenderStackContainer
import com.carbroz.runtime.sdui.rendering.RenderableSduiDefinition
import com.carbroz.runtime.sdui.rendering.SduiRenderContext
import kotlinx.serialization.json.JsonObject

object StackSectionDefinition : SectionDefinition<StackContainerProperties>, RenderableSduiDefinition {
    override val type: NodeType = NodeType("STACK")
    override fun decodeProperties(raw: JsonObject): PropertyDecodeResult<StackContainerProperties> =
        StackContainerPropertiesDecoder.decode(raw)

    @Composable
    override fun RenderSection(node: Section, context: SduiRenderContext, children: @Composable () -> Unit): Boolean {
        val properties = node.properties as? StackContainerProperties ?: return false
        RenderStackContainer(properties, children)
        return true
    }
}
