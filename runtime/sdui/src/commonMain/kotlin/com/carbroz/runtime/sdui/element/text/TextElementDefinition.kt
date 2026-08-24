package com.carbroz.runtime.sdui.element.text

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import com.carbroz.runtime.sdui.extension.ElementDefinition
import com.carbroz.runtime.sdui.extension.PropertyDecodeResult
import com.carbroz.runtime.sdui.model.Element
import com.carbroz.runtime.sdui.model.NodeProperties
import com.carbroz.runtime.sdui.model.NodeType
import com.carbroz.runtime.sdui.rendering.RenderableSduiDefinition
import com.carbroz.runtime.sdui.rendering.SduiRenderContext
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull

enum class TextStyleToken {
    TITLE_LARGE,
    BODY_LARGE,
    BODY_MEDIUM,
    LABEL_LARGE,
}

data class TextElementProperties(
    val text: String,
    val style: TextStyleToken,
) : NodeProperties

object TextElementDefinition : ElementDefinition<TextElementProperties>, RenderableSduiDefinition {
    override val type: NodeType = NodeType("TEXT")

    override fun decodeProperties(raw: JsonObject): PropertyDecodeResult<TextElementProperties> {
        val text = (raw["text"] as? JsonPrimitive)?.contentOrNull
            ?: return PropertyDecodeResult.Failure("TEXT requires string 'text'.")
        val styleValue = (raw["style"] as? JsonPrimitive)?.contentOrNull ?: TextStyleToken.BODY_MEDIUM.name
        val style = TextStyleToken.entries.firstOrNull { it.name == styleValue.uppercase() }
            ?: return PropertyDecodeResult.Failure("Unsupported TEXT style '$styleValue'.")
        return PropertyDecodeResult.Success(TextElementProperties(text, style))
    }

    @Composable
    override fun RenderElement(node: Element, context: SduiRenderContext): Boolean {
        val properties = node.properties as? TextElementProperties ?: return false
        val style = when (properties.style) {
            TextStyleToken.TITLE_LARGE -> MaterialTheme.typography.titleLarge
            TextStyleToken.BODY_LARGE -> MaterialTheme.typography.bodyLarge
            TextStyleToken.BODY_MEDIUM -> MaterialTheme.typography.bodyMedium
            TextStyleToken.LABEL_LARGE -> MaterialTheme.typography.labelLarge
        }
        Text(text = properties.text, style = style)
        return true
    }
}
