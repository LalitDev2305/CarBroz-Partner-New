package com.carbroz.runtime.sdui.component.stack

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.carbroz.runtime.sdui.extension.ComponentDefinition
import com.carbroz.runtime.sdui.extension.PropertyDecodeResult
import com.carbroz.runtime.sdui.model.Component
import com.carbroz.runtime.sdui.model.NodeType
import com.carbroz.runtime.sdui.properties.StackAxis
import com.carbroz.runtime.sdui.properties.StackContainerProperties
import com.carbroz.runtime.sdui.properties.StackContainerPropertiesDecoder
import com.carbroz.runtime.sdui.properties.StackCrossAxisAlignment
import com.carbroz.runtime.sdui.properties.StackMainAxisAlignment
import com.carbroz.runtime.sdui.rendering.RenderableSduiDefinition
import com.carbroz.runtime.sdui.rendering.SduiRenderContext
import com.carbroz.runtime.sdui.rendering.applyCommonNodeProperties
import kotlinx.serialization.json.JsonObject

object StackComponentDefinition : ComponentDefinition<StackContainerProperties>, RenderableSduiDefinition {
    override val type: NodeType = NodeType("STACK")
    override fun decodeProperties(raw: JsonObject): PropertyDecodeResult<StackContainerProperties> = StackContainerPropertiesDecoder.decode(raw)

    @Composable
    override fun RenderComponent(node: Component, context: SduiRenderContext, children: @Composable () -> Unit): Boolean {
        val properties = node.properties as? StackContainerProperties ?: return false
        if (!properties.common.visible) return true
        val modifier = Modifier.applyCommonNodeProperties(properties.common)
        when (properties.axis) {
            StackAxis.VERTICAL -> Column(
                modifier = modifier,
                verticalArrangement = verticalArrangement(properties),
                horizontalAlignment = horizontalAlignment(properties.crossAxisAlignment),
                content = { children() },
            )
            StackAxis.HORIZONTAL -> Row(
                modifier = modifier,
                horizontalArrangement = horizontalArrangement(properties),
                verticalAlignment = verticalAlignment(properties.crossAxisAlignment),
                content = { children() },
            )
        }
        return true
    }

    private fun verticalArrangement(p: StackContainerProperties): Arrangement.Vertical = if (p.spacingDp > 0f) Arrangement.spacedBy(p.spacingDp.dp) else when (p.mainAxisAlignment) {
        StackMainAxisAlignment.START -> Arrangement.Top
        StackMainAxisAlignment.CENTER -> Arrangement.Center
        StackMainAxisAlignment.END -> Arrangement.Bottom
        StackMainAxisAlignment.SPACE_BETWEEN -> Arrangement.SpaceBetween
        StackMainAxisAlignment.SPACE_AROUND -> Arrangement.SpaceAround
        StackMainAxisAlignment.SPACE_EVENLY -> Arrangement.SpaceEvenly
    }
    private fun horizontalArrangement(p: StackContainerProperties): Arrangement.Horizontal = if (p.spacingDp > 0f) Arrangement.spacedBy(p.spacingDp.dp) else when (p.mainAxisAlignment) {
        StackMainAxisAlignment.START -> Arrangement.Start
        StackMainAxisAlignment.CENTER -> Arrangement.Center
        StackMainAxisAlignment.END -> Arrangement.End
        StackMainAxisAlignment.SPACE_BETWEEN -> Arrangement.SpaceBetween
        StackMainAxisAlignment.SPACE_AROUND -> Arrangement.SpaceAround
        StackMainAxisAlignment.SPACE_EVENLY -> Arrangement.SpaceEvenly
    }
    private fun horizontalAlignment(value: StackCrossAxisAlignment): Alignment.Horizontal = when (value) {
        StackCrossAxisAlignment.START, StackCrossAxisAlignment.STRETCH -> Alignment.Start
        StackCrossAxisAlignment.CENTER -> Alignment.CenterHorizontally
        StackCrossAxisAlignment.END -> Alignment.End
    }
    private fun verticalAlignment(value: StackCrossAxisAlignment): Alignment.Vertical = when (value) {
        StackCrossAxisAlignment.START, StackCrossAxisAlignment.STRETCH -> Alignment.Top
        StackCrossAxisAlignment.CENTER -> Alignment.CenterVertically
        StackCrossAxisAlignment.END -> Alignment.Bottom
    }
}
