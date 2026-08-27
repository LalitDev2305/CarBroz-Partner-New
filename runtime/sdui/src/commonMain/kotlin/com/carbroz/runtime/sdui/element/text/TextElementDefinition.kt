package com.carbroz.runtime.sdui.element.text

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import com.carbroz.runtime.sdui.extension.ElementDefinition
import com.carbroz.runtime.sdui.extension.PropertyDecodeResult
import com.carbroz.runtime.sdui.model.Element
import com.carbroz.runtime.sdui.model.NodeType
import com.carbroz.runtime.sdui.properties.CommonNodeProperties
import com.carbroz.runtime.sdui.properties.CommonNodePropertiesDecoder
import com.carbroz.runtime.sdui.properties.CommonNodePropertyOwner
import com.carbroz.runtime.sdui.properties.CommonPropertiesDecodeResult
import com.carbroz.runtime.sdui.rendering.RenderableSduiDefinition
import com.carbroz.runtime.sdui.rendering.SduiRenderContext
import com.carbroz.runtime.sdui.rendering.applyCommonNodeProperties
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull

enum class TextStyleToken { TITLE_LARGE, BODY_LARGE, BODY_MEDIUM, LABEL_LARGE }
enum class TextAlignmentToken { START, CENTER, END }

data class TextElementProperties(
    override val common: CommonNodeProperties,
    val text: String,
    val style: TextStyleToken,
    val alignment: TextAlignmentToken,
    val maxLines: Int?,
) : CommonNodePropertyOwner

object TextElementDefinition : ElementDefinition<TextElementProperties>, RenderableSduiDefinition {
    override val type: NodeType = NodeType("TEXT")

    override fun decodeProperties(raw: JsonObject): PropertyDecodeResult<TextElementProperties> {
        val common = when (val decoded = CommonNodePropertiesDecoder.decode(raw)) {
            is CommonPropertiesDecodeResult.Success -> decoded.value
            is CommonPropertiesDecodeResult.Failure -> return PropertyDecodeResult.Failure(decoded.reason)
        }
        val text = (raw["text"] as? JsonPrimitive)?.contentOrNull
            ?: return PropertyDecodeResult.Failure("TEXT requires string 'text'.")
        val styleValue = (raw["style"] as? JsonPrimitive)?.contentOrNull ?: TextStyleToken.BODY_MEDIUM.name
        val style = TextStyleToken.entries.firstOrNull { it.name == styleValue.uppercase() }
            ?: return PropertyDecodeResult.Failure("Unsupported TEXT style '$styleValue'.")
        val alignmentValue = (raw["textAlignment"] as? JsonPrimitive)?.contentOrNull ?: TextAlignmentToken.START.name
        val alignment = TextAlignmentToken.entries.firstOrNull { it.name == alignmentValue.uppercase() }
            ?: return PropertyDecodeResult.Failure("Unsupported TEXT textAlignment '$alignmentValue'.")
        val maxLines = (raw["maxLines"] as? JsonPrimitive)?.intOrNull
        if (maxLines != null && maxLines <= 0) return PropertyDecodeResult.Failure("TEXT 'maxLines' must be positive.")
        return PropertyDecodeResult.Success(TextElementProperties(common, text, style, alignment, maxLines))
    }

    @Composable
    override fun RenderElement(node: Element, context: SduiRenderContext): Boolean {
        val properties = node.properties as? TextElementProperties ?: return false
        if (!properties.common.visible) return true
        val style = when (properties.style) {
            TextStyleToken.TITLE_LARGE -> MaterialTheme.typography.titleLarge
            TextStyleToken.BODY_LARGE -> MaterialTheme.typography.bodyLarge
            TextStyleToken.BODY_MEDIUM -> MaterialTheme.typography.bodyMedium
            TextStyleToken.LABEL_LARGE -> MaterialTheme.typography.labelLarge
        }
        val textAlign = when (properties.alignment) {
            TextAlignmentToken.START -> TextAlign.Start
            TextAlignmentToken.CENTER -> TextAlign.Center
            TextAlignmentToken.END -> TextAlign.End
        }
        Text(
            text = properties.text,
            modifier = Modifier.applyCommonNodeProperties(properties.common),
            style = style,
            textAlign = textAlign,
            maxLines = properties.maxLines ?: Int.MAX_VALUE,
        )
        return true
    }
}
