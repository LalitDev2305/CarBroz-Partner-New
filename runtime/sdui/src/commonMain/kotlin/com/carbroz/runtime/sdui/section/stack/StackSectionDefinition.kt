package com.carbroz.runtime.sdui.section.stack

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.carbroz.foundation.designsystem.CarBrozDesignSystem
import com.carbroz.runtime.sdui.extension.PropertyDecodeResult
import com.carbroz.runtime.sdui.extension.SectionDefinition
import com.carbroz.runtime.sdui.model.NodeProperties
import com.carbroz.runtime.sdui.model.NodeType
import com.carbroz.runtime.sdui.model.Section
import com.carbroz.runtime.sdui.rendering.RenderableSduiDefinition
import com.carbroz.runtime.sdui.rendering.SduiRenderContext
import kotlinx.serialization.json.JsonObject

/** Default vertical Section container. */
data object StackSectionProperties : NodeProperties

object StackSectionDefinition : SectionDefinition<StackSectionProperties>, RenderableSduiDefinition {
    override val type: NodeType = NodeType("STACK")

    override fun decodeProperties(raw: JsonObject): PropertyDecodeResult<StackSectionProperties> =
        PropertyDecodeResult.Success(StackSectionProperties)

    @Composable
    override fun RenderSection(
        node: Section,
        context: SduiRenderContext,
        children: @Composable () -> Unit,
    ): Boolean {
        if (node.properties !== StackSectionProperties) return false
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(CarBrozDesignSystem.spacing.medium),
        ) {
            children()
        }
        return true
    }
}
