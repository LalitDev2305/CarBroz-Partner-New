package com.carbroz.sdui.render.element

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.carbroz.sdui.model.SduiElement
import com.carbroz.sdui.parser.SduiDecoder
import com.carbroz.sdui.registry.ElementRenderer
import com.carbroz.sdui.render.SduiInteraction
import com.carbroz.sdui.render.SduiRenderContext
import com.carbroz.sdui.render.accessories
import com.carbroz.sdui.render.accessory.AccessoryRenderer
import com.carbroz.sdui.render.arrayValue
import com.carbroz.sdui.render.boolean
import com.carbroz.sdui.render.float
import com.carbroz.sdui.render.modifier.applySduiProperties
import com.carbroz.sdui.render.modifier.parseSduiColor
import com.carbroz.sdui.render.resolvedContent
import com.carbroz.sdui.render.string
import kotlinx.serialization.json.JsonObject

object TextElementRenderer : ElementRenderer {
    override val type: String = "text"
    private val decoder = SduiDecoder()

    @Composable
    override fun Render(node: SduiElement, context: SduiRenderContext) {
        val runtime = context.nodeStates[node.id]
        if (runtime?.visible == false) return
        val enabled = runtime?.enabled ?: node.properties.boolean("enabled") ?: true
        val leading = node.properties.accessories("leading")
        val trailing = node.properties.accessories("trailing")

        Row(modifier = Modifier.applySduiProperties(node.properties)) {
            leading.forEach {
                AccessoryRenderer.Render(it, context)
                Spacer(Modifier.width(6.dp))
            }
            val spans = node.properties.arrayValue("spans")
            if (spans != null) {
                FlowRow {
                    spans.forEachIndexed { index, item ->
                        val span = item as? JsonObject ?: return@forEachIndexed
                        val text = span["text"].resolvedContent(context).orEmpty()
                        val action = span["onClick"]?.let(decoder::decodeAction)
                        Text(
                            text = text,
                            color = span.string("color")?.let(::parseSduiColor)
                                ?: node.properties.string(if (enabled) "color" else "disabledColor")?.let(::parseSduiColor)
                                ?: androidx.compose.ui.graphics.Color.Unspecified,
                            fontSize = (node.properties.float("fontSize") ?: 14f).sp,
                            fontWeight = FontWeight((span.float("fontWeight") ?: node.properties.float("fontWeight") ?: 400f).toInt()),
                            textDecoration = if (span.boolean("underline") == true) TextDecoration.Underline else null,
                            modifier = if (enabled && action != null) Modifier.clickable {
                                context.onInteraction(SduiInteraction.ActionTriggered("${node.id}:span:$index", "onClick", action))
                            } else Modifier,
                        )
                    }
                }
            } else {
                val text = node.properties["text"].resolvedContent(context).orEmpty()
                val action = node.actions["onClick"]
                Text(
                    text = text,
                    color = node.properties.string(if (enabled) "color" else "disabledColor")?.let(::parseSduiColor)
                        ?: androidx.compose.ui.graphics.Color.Unspecified,
                    fontSize = (node.properties.float("fontSize") ?: 14f).sp,
                    fontWeight = FontWeight((node.properties.float("fontWeight") ?: 400f).toInt()),
                    lineHeight = (node.properties.float("lineHeight") ?: 0f).takeIf { it > 0f }?.sp ?: TextUnit.Unspecified,
                    letterSpacing = (node.properties.float("letterSpacing") ?: 0f).sp,
                    textAlign = node.properties.string("textAlign").toTextAlign(),
                    modifier = if (enabled && action != null) Modifier.clickable {
                        context.onInteraction(SduiInteraction.ActionTriggered(node.id, "onClick", action))
                    } else Modifier,
                )
            }
            trailing.forEach {
                Spacer(Modifier.width(6.dp))
                AccessoryRenderer.Render(it, context)
            }
        }
    }

    private fun String?.toTextAlign(): TextAlign = when (this) {
        "center" -> TextAlign.Center
        "end" -> TextAlign.End
        "justify" -> TextAlign.Justify
        else -> TextAlign.Start
    }
}
