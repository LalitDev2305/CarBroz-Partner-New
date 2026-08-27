package com.carbroz.runtime.sdui.element.input

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull

data class InputElementProperties(
    val fieldId: String,
    val initialValue: String,
    val label: String?,
    val required: Boolean,
    val fillWidth: Boolean,
) : NodeProperties

/** Generic text input. Business meaning is supplied only by server fieldId/data. */
object InputElementDefinition : ElementDefinition<InputElementProperties>, RenderableSduiDefinition {
    override val type: NodeType = NodeType("INPUT")

    override fun decodeProperties(raw: JsonObject): PropertyDecodeResult<InputElementProperties> {
        val fieldId = (raw["fieldId"] as? JsonPrimitive)?.contentOrNull?.trim()
            ?: return PropertyDecodeResult.Failure("INPUT requires string 'fieldId'.")
        if (fieldId.isBlank()) return PropertyDecodeResult.Failure("INPUT 'fieldId' must not be blank.")
        val initial = (raw["initialValue"] as? JsonPrimitive)?.contentOrNull.orEmpty()
        val label = (raw["label"] as? JsonPrimitive)?.contentOrNull
        val required = (raw["required"] as? JsonPrimitive)?.booleanOrNull ?: false
        val fillWidth = (raw["fillWidth"] as? JsonPrimitive)?.booleanOrNull ?: true
        return PropertyDecodeResult.Success(InputElementProperties(fieldId, initial, label, required, fillWidth))
    }

    @Composable
    override fun RenderElement(node: Element, context: SduiRenderContext): Boolean {
        val properties = node.properties as? InputElementProperties ?: return false
        var value by remember(node.path, properties.initialValue) { mutableStateOf(properties.initialValue) }
        OutlinedTextField(
            value = value,
            onValueChange = {
                value = it
                context.events.emit(SduiRenderEvent.ValueChanged(node.path, it))
            },
            modifier = if (properties.fillWidth) Modifier.fillMaxWidth() else Modifier,
            label = properties.label?.let { { Text(it) } },
            singleLine = true,
        )
        return true
    }
}
