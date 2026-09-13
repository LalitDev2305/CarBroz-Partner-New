package com.carbroz.sdui.render.element

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.carbroz.sdui.model.SduiElement
import com.carbroz.sdui.registry.ElementRenderer
import com.carbroz.sdui.render.SduiInteraction
import com.carbroz.sdui.render.SduiRenderContext
import com.carbroz.sdui.render.accessories
import com.carbroz.sdui.render.accessory.AccessoryRenderer
import com.carbroz.sdui.render.boolean
import com.carbroz.sdui.render.modifier.applySduiProperties
import com.carbroz.sdui.render.resolvedContent
import com.carbroz.sdui.render.string

object ImageElementRenderer : ElementRenderer {
    override val type: String = "image"

    @Composable
    override fun Render(node: SduiElement, context: SduiRenderContext) {
        val runtime = context.nodeStates[node.id]
        if (runtime?.visible == false) return
        val enabled = runtime?.enabled ?: node.properties.boolean("enabled") ?: true
        val action = node.actions["onClick"]
        val imageModifier = Modifier
            .applySduiProperties(node.properties)
            .then(if (enabled && action != null) Modifier.clickable {
                context.onInteraction(SduiInteraction.ActionTriggered(node.id, "onClick", action))
            } else Modifier)
        val url = node.properties["url"].resolvedContent(context) ?: return
        val contentDescription = node.accessibility?.get("label").resolvedContent(context)
        Row {
            node.properties.accessories("leading").forEach {
                AccessoryRenderer.Render(it, context)
                Spacer(Modifier.width(6.dp))
            }
            AsyncImage(
                model = context.resolveAssetUrl(url),
                contentDescription = contentDescription,
                modifier = imageModifier,
                contentScale = when (node.properties.string("contentScale")) {
                    "crop" -> ContentScale.Crop
                    "fillBounds" -> ContentScale.FillBounds
                    "inside" -> ContentScale.Inside
                    else -> ContentScale.Fit
                },
            )
            node.properties.accessories("trailing").forEach {
                Spacer(Modifier.width(6.dp))
                AccessoryRenderer.Render(it, context)
            }
        }
    }
}
