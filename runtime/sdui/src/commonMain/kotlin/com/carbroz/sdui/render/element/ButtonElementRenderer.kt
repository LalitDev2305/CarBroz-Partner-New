package com.carbroz.sdui.render.element

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.carbroz.sdui.model.SduiElement
import com.carbroz.sdui.registry.ElementRenderer
import com.carbroz.sdui.render.SduiInteraction
import com.carbroz.sdui.render.SduiRenderContext
import com.carbroz.sdui.render.accessories
import com.carbroz.sdui.render.accessory.AccessoryRenderer
import com.carbroz.sdui.render.boolean
import com.carbroz.sdui.render.float
import com.carbroz.sdui.render.modifier.applySduiProperties
import com.carbroz.sdui.render.modifier.parseSduiColor
import com.carbroz.sdui.render.resolvedContent
import com.carbroz.sdui.render.string

object ButtonElementRenderer : ElementRenderer {
    override val type: String = "button"

    @Composable
    override fun Render(node: SduiElement, context: SduiRenderContext) {
        val runtime = context.nodeStates[node.id]
        if (runtime?.visible == false) return
        val enabled = runtime?.enabled ?: node.properties.boolean("enabled") ?: true
        val loading = runtime?.loading == true
        val action = node.actions["onClick"]
        val modifier = Modifier
            .applySduiProperties(node.properties)
            .clickable(enabled = enabled && !loading && action != null) {
                action?.let { context.onInteraction(SduiInteraction.ActionTriggered(node.id, "onClick", it)) }
            }

        Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
            node.properties.accessories("leading").forEach {
                AccessoryRenderer.Render(it, context)
                Spacer(Modifier.width(8.dp))
            }
            if (loading) {
                CircularProgressIndicator(modifier = Modifier.width(20.dp))
            } else {
                Text(
                    text = node.properties["text"].resolvedContent(context).orEmpty(),
                    color = node.properties.string("textColor")?.let(::parseSduiColor)
                        ?: androidx.compose.ui.graphics.Color.Unspecified,
                    fontSize = (node.properties.float("fontSize") ?: 16f).sp,
                    fontWeight = FontWeight((node.properties.float("fontWeight") ?: 500f).toInt()),
                )
            }
            node.properties.accessories("trailing").forEach {
                Spacer(Modifier.width(8.dp))
                AccessoryRenderer.Render(it, context)
            }
        }
    }
}
