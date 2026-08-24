package com.carbroz.runtime.sdui.group.stack

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.carbroz.foundation.designsystem.CarBrozDesignSystem
import com.carbroz.runtime.sdui.extension.GroupDefinition
import com.carbroz.runtime.sdui.extension.PropertyDecodeResult
import com.carbroz.runtime.sdui.model.Group
import com.carbroz.runtime.sdui.model.NodeProperties
import com.carbroz.runtime.sdui.model.NodeType
import com.carbroz.runtime.sdui.rendering.RenderableSduiDefinition
import com.carbroz.runtime.sdui.rendering.SduiRenderContext
import kotlinx.serialization.json.JsonObject

/** Default vertical Group container. */
data object StackGroupProperties : NodeProperties

object StackGroupDefinition : GroupDefinition<StackGroupProperties>, RenderableSduiDefinition {
    override val type: NodeType = NodeType("STACK")

    override fun decodeProperties(raw: JsonObject): PropertyDecodeResult<StackGroupProperties> =
        PropertyDecodeResult.Success(StackGroupProperties)

    @Composable
    override fun RenderGroup(
        node: Group,
        context: SduiRenderContext,
        children: @Composable () -> Unit,
    ): Boolean {
        if (node.properties !== StackGroupProperties) return false
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(CarBrozDesignSystem.spacing.medium),
        ) {
            children()
        }
        return true
    }
}
