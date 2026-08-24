package com.carbroz.runtime.sdui.element.button

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.carbroz.runtime.sdui.extension.ElementDefinition
import com.carbroz.runtime.sdui.extension.PropertyDecodeResult
import com.carbroz.runtime.sdui.model.Element
import com.carbroz.runtime.sdui.model.NodeProperties
import com.carbroz.runtime.sdui.model.NodeType
import com.carbroz.runtime.sdui.rendering.RenderableSduiDefinition
import com.carbroz.runtime.sdui.rendering.SduiRenderContext
import com.carbroz.runtime.sdui.rendering.SduiRenderEvent
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive

data class ButtonElementProperties(
    val text: String,
    val fillWidth: Boolean,
) : NodeProperties

object ButtonElementDefinition : ElementDefinition<ButtonElementProperties>, RenderableSduiDefinition {
    override val type: NodeType = NodeType("BUTTON")

    override fun decodeProperties(raw: JsonObject): PropertyDecodeResult<ButtonElementProperties> {
        val text = raw["text"]?.jsonPrimitive?.contentOrNull
            ?: return PropertyDecodeResult.Failure("BUTTON requires non-null 'text'.")
        val fillWidth = raw["fillWidth"]?.jsonPrimitive?.contentOrNull?.toBooleanStrictOrNull() ?: true
        return PropertyDecodeResult.Success(ButtonElementProperties(text = text, fillWidth = fillWidth))
    }

    @Composable
    override fun RenderElement(node: Element, context: SduiRenderContext): Boolean {
        val properties = node.properties as? ButtonElementProperties ?: return false
        val modifier = if (properties.fillWidth) Modifier.fillMaxWidth() else Modifier
        Button(
            modifier = modifier,
            enabled = node.command != null,
            onClick = { context.events.emit(SduiRenderEvent.Activated(node.path)) },
        ) {
            Text(properties.text)
        }
        return true
    }
}
