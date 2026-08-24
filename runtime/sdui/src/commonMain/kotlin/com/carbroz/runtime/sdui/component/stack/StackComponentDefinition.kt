package com.carbroz.runtime.sdui.component.stack

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.carbroz.foundation.designsystem.CarBrozDesignSystem
import com.carbroz.runtime.sdui.extension.ComponentDefinition
import com.carbroz.runtime.sdui.extension.PropertyDecodeResult
import com.carbroz.runtime.sdui.model.Component
import com.carbroz.runtime.sdui.model.NodeProperties
import com.carbroz.runtime.sdui.model.NodeType
import com.carbroz.runtime.sdui.rendering.RenderableSduiDefinition
import com.carbroz.runtime.sdui.rendering.SduiRenderContext
import kotlinx.serialization.json.JsonObject

/** Default vertical structural container. */
data object StackComponentProperties : NodeProperties

object StackComponentDefinition : ComponentDefinition<StackComponentProperties>, RenderableSduiDefinition {
    override val type: NodeType = NodeType("STACK")

    override fun decodeProperties(raw: JsonObject): PropertyDecodeResult<StackComponentProperties> =
        PropertyDecodeResult.Success(StackComponentProperties)

    @Composable
    override fun RenderComponent(
        node: Component,
        context: SduiRenderContext,
        children: @Composable () -> Unit,
    ): Boolean {
        if (node.properties !== StackComponentProperties) return false
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(CarBrozDesignSystem.spacing.medium),
        ) {
            children()
        }
        return true
    }
}
