package com.carbroz.runtime.sdui.element.button

import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
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
import com.carbroz.runtime.sdui.rendering.SduiRenderEvent
import com.carbroz.runtime.sdui.rendering.applyCommonNodeProperties
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull

data class ButtonElementProperties(
    override val common: CommonNodeProperties,
    val text: String,
    val enabled: Boolean,
    val loading: Boolean,
) : CommonNodePropertyOwner

object ButtonElementDefinition : ElementDefinition<ButtonElementProperties>, RenderableSduiDefinition {
    override val type: NodeType = NodeType("BUTTON")

    override fun decodeProperties(raw: JsonObject): PropertyDecodeResult<ButtonElementProperties> {
        val common = when (val decoded = CommonNodePropertiesDecoder.decode(raw)) {
            is CommonPropertiesDecodeResult.Success -> decoded.value
            is CommonPropertiesDecodeResult.Failure -> return PropertyDecodeResult.Failure(decoded.reason)
        }
        val text = (raw["text"] as? JsonPrimitive)?.contentOrNull
            ?: return PropertyDecodeResult.Failure("BUTTON requires string 'text'.")
        val enabled = (raw["enabled"] as? JsonPrimitive)?.booleanOrNull ?: true
        val loading = (raw["loading"] as? JsonPrimitive)?.booleanOrNull ?: false
        return PropertyDecodeResult.Success(ButtonElementProperties(common, text, enabled, loading))
    }

    @Composable
    override fun RenderElement(node: Element, context: SduiRenderContext): Boolean {
        val properties = node.properties as? ButtonElementProperties ?: return false
        if (!properties.common.visible) return true
        Button(
            modifier = Modifier.applyCommonNodeProperties(properties.common),
            enabled = properties.enabled && !properties.loading && node.command != null,
            onClick = { context.events.emit(SduiRenderEvent.Activated(node.path)) },
        ) {
            if (properties.loading) CircularProgressIndicator() else Text(properties.text)
        }
        return true
    }
}
