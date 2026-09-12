package com.carbroz.sdui.render.element

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.carbroz.sdui.model.SduiElement
import com.carbroz.sdui.registry.ElementRenderer
import com.carbroz.sdui.render.SduiInteraction
import com.carbroz.sdui.render.SduiRenderContext
import com.carbroz.sdui.render.accessories
import com.carbroz.sdui.render.accessory.AccessoryRenderer
import com.carbroz.sdui.render.float
import com.carbroz.sdui.render.int
import com.carbroz.sdui.render.modifier.applySduiProperties
import com.carbroz.sdui.render.modifier.parseSduiColor
import com.carbroz.sdui.render.objectValue
import com.carbroz.sdui.render.string
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull

object InputElementRenderer : ElementRenderer {
    override val type: String = "input"

    @Composable
    override fun Render(node: SduiElement, context: SduiRenderContext) {
        val runtime = context.nodeStates[node.id]
        if (runtime?.visible == false) return
        val bindingKey = node.binding?.key ?: return
        val field = context.fields[bindingKey]
        val runtimeValue = runtime?.value as? JsonPrimitive
        val value = runtimeValue?.contentOrNull ?: (field?.value as? JsonPrimitive)?.contentOrNull.orEmpty()
        val enabled = runtime?.enabled ?: true
        val maxLength = node.properties.int("maxLength")
        val keyboardType = when (node.properties.string("keyboardType")) {
            "phone" -> KeyboardType.Phone
            "number" -> KeyboardType.Number
            "email" -> KeyboardType.Email
            "password" -> KeyboardType.Password
            else -> KeyboardType.Text
        }
        val onValueChange: (String) -> Unit = { candidate ->
            val next = maxLength?.let { candidate.take(it) } ?: candidate
            context.onInteraction(
                SduiInteraction.ValueChanged(
                    elementId = node.id,
                    bindingKey = bindingKey,
                    value = JsonPrimitive(next),
                ),
            )
        }
        val modifier = Modifier
            .applySduiProperties(node.properties)
            .then(if (node.properties.float("weight") != null) Modifier.fillMaxWidth() else Modifier)

        Column(modifier = modifier) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                node.properties.accessories("leading").forEach {
                    AccessoryRenderer.Render(it, context)
                    Spacer(Modifier.width(8.dp))
                }

                val presentation = node.properties.objectValue("presentation")
                if (presentation?.string("type") == "segmented") {
                    SegmentedInput(
                        value = value,
                        enabled = enabled,
                        count = presentation.int("count") ?: maxLength ?: 6,
                        keyboardType = keyboardType,
                        properties = presentation,
                        onValueChange = onValueChange,
                    )
                } else {
                    OutlinedTextField(
                        value = value,
                        onValueChange = onValueChange,
                        enabled = enabled,
                        singleLine = true,
                        placeholder = node.properties.string("placeholder")?.let { { Text(it) } },
                        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
                        visualTransformation = if (keyboardType == KeyboardType.Password) PasswordVisualTransformation() else VisualTransformation.None,
                        isError = field?.error != null,
                    )
                }

                node.properties.accessories("trailing").forEach {
                    Spacer(Modifier.width(8.dp))
                    AccessoryRenderer.Render(it, context)
                }
            }

            field?.error?.let { error ->
                Spacer(Modifier.height(4.dp))
                Text(error, color = androidx.compose.material3.MaterialTheme.colorScheme.error)
            }
        }
    }

    @Composable
    private fun SegmentedInput(
        value: String,
        enabled: Boolean,
        count: Int,
        keyboardType: KeyboardType,
        properties: kotlinx.serialization.json.JsonObject,
        onValueChange: (String) -> Unit,
    ) {
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            enabled = enabled,
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
            decorationBox = { innerTextField ->
                Box {
                    Row {
                        repeat(count) { index ->
                            if (index > 0) Spacer(Modifier.width((properties.float("spacing") ?: 8f).dp))
                            val background = properties.objectValue("background")?.string("color")?.let(::parseSduiColor)
                            val border = properties.objectValue("border")
                            val borderColor = border?.string("color")?.let(::parseSduiColor)
                            val borderWidth = (border?.float("width") ?: 1f).dp
                            val radius = properties.objectValue("shape")?.float("cornerRadius") ?: 12f
                            var cell = Modifier
                                .width((properties.float("segmentWidth") ?: 44f).dp)
                                .height((properties.float("segmentHeight") ?: 52f).dp)
                            if (background != null) cell = cell.background(background, RoundedCornerShape(radius.dp))
                            if (borderColor != null) cell = cell.border(borderWidth, borderColor, RoundedCornerShape(radius.dp))
                            Box(cell, contentAlignment = Alignment.Center) {
                                Text(value.getOrNull(index)?.toString().orEmpty())
                            }
                        }
                    }
                    Box(Modifier.width(1.dp).height(1.dp)) { innerTextField() }
                }
            },
        )
    }
}
