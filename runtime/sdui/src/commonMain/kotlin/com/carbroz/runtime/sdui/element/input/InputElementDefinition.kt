package com.carbroz.runtime.sdui.element.input

import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import com.carbroz.runtime.sdui.extension.ElementDefinition
import com.carbroz.runtime.sdui.extension.FormFieldContribution
import com.carbroz.runtime.sdui.extension.FormFieldContributor
import com.carbroz.runtime.sdui.extension.PropertyDecodeResult
import com.carbroz.runtime.sdui.model.Element
import com.carbroz.runtime.sdui.model.NodeProperties
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
import kotlinx.serialization.json.intOrNull

enum class InputKeyboardType { TEXT, PHONE, NUMBER, EMAIL, URI, PASSWORD }

data class InputElementProperties(
    override val common: CommonNodeProperties,
    val fieldId: String,
    val initialValue: String,
    val label: String?,
    val placeholder: String?,
    val required: Boolean,
    val enabled: Boolean,
    val readOnly: Boolean,
    val maxLength: Int?,
    val keyboardType: InputKeyboardType,
) : CommonNodePropertyOwner

object InputElementDefinition : ElementDefinition<InputElementProperties>, RenderableSduiDefinition, FormFieldContributor {
    override val type: NodeType = NodeType("INPUT")

    override fun decodeProperties(raw: JsonObject): PropertyDecodeResult<InputElementProperties> {
        val common = when (
            val decoded = CommonNodePropertiesDecoder.decode(raw, CommonNodeProperties(fillWidth = true))
        ) {
            is CommonPropertiesDecodeResult.Success -> decoded.value
            is CommonPropertiesDecodeResult.Failure -> return PropertyDecodeResult.Failure(decoded.reason)
        }
        val fieldId = (raw["fieldId"] as? JsonPrimitive)?.contentOrNull?.trim()
            ?: return PropertyDecodeResult.Failure("INPUT requires string 'fieldId'.")
        if (fieldId.isBlank()) return PropertyDecodeResult.Failure("INPUT 'fieldId' must not be blank.")
        val initial = (raw["initialValue"] as? JsonPrimitive)?.contentOrNull.orEmpty()
        val label = (raw["label"] as? JsonPrimitive)?.contentOrNull
        val placeholder = (raw["placeholder"] as? JsonPrimitive)?.contentOrNull
        val required = (raw["required"] as? JsonPrimitive)?.booleanOrNull ?: false
        val enabled = (raw["enabled"] as? JsonPrimitive)?.booleanOrNull ?: true
        val readOnly = (raw["readOnly"] as? JsonPrimitive)?.booleanOrNull ?: false
        val maxLength = (raw["maxLength"] as? JsonPrimitive)?.intOrNull
        if (maxLength != null && maxLength <= 0) return PropertyDecodeResult.Failure("INPUT 'maxLength' must be positive.")
        val keyboardValue = (raw["keyboardType"] as? JsonPrimitive)?.contentOrNull ?: InputKeyboardType.TEXT.name
        val keyboardType = InputKeyboardType.entries.firstOrNull { it.name == keyboardValue.uppercase() }
            ?: return PropertyDecodeResult.Failure("Unsupported INPUT keyboardType '$keyboardValue'.")
        return PropertyDecodeResult.Success(
            InputElementProperties(common, fieldId, initial, label, placeholder, required, enabled, readOnly, maxLength, keyboardType),
        )
    }

    override fun formFieldContribution(properties: NodeProperties): FormFieldContribution? {
        val input = properties as? InputElementProperties ?: return null
        return FormFieldContribution(input.fieldId, JsonPrimitive(input.initialValue), input.required)
    }

    @Composable
    override fun RenderElement(node: Element, context: SduiRenderContext): Boolean {
        val properties = node.properties as? InputElementProperties ?: return false
        if (!properties.common.visible) return true
        val restored = (context.values.resolve(node.path, properties.fieldId) as? JsonPrimitive)?.contentOrNull
            ?: properties.initialValue
        var value by remember(node.path, restored) { mutableStateOf(restored) }
        OutlinedTextField(
            value = value,
            onValueChange = { candidate ->
                val next = properties.maxLength?.let { candidate.take(it) } ?: candidate
                value = next
                context.events.emit(SduiRenderEvent.ValueChanged(node.path, next, properties.fieldId))
            },
            modifier = Modifier.applyCommonNodeProperties(properties.common),
            label = properties.label?.let { { Text(it) } },
            placeholder = properties.placeholder?.let { { Text(it) } },
            singleLine = true,
            enabled = properties.enabled,
            readOnly = properties.readOnly,
            keyboardOptions = KeyboardOptions(keyboardType = properties.keyboardType.toComposeKeyboardType()),
        )
        return true
    }

    private fun InputKeyboardType.toComposeKeyboardType(): KeyboardType = when (this) {
        InputKeyboardType.TEXT -> KeyboardType.Text
        InputKeyboardType.PHONE -> KeyboardType.Phone
        InputKeyboardType.NUMBER -> KeyboardType.Number
        InputKeyboardType.EMAIL -> KeyboardType.Email
        InputKeyboardType.URI -> KeyboardType.Uri
        InputKeyboardType.PASSWORD -> KeyboardType.Password
    }
}
