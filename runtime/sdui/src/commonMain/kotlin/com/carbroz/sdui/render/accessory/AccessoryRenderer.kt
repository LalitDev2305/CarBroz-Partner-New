package com.carbroz.sdui.render.accessory

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.carbroz.sdui.model.SduiAccessory
import com.carbroz.sdui.render.SduiRenderContext
import com.carbroz.sdui.render.float
import com.carbroz.sdui.render.modifier.parseSduiColor
import com.carbroz.sdui.render.resolvedContent
import com.carbroz.sdui.render.string

object AccessoryRenderer {
    private val supportedTypes = setOf("icon", "divider", "image")

    fun supports(type: String): Boolean = type in supportedTypes

    @Composable
    fun Render(accessory: SduiAccessory, context: SduiRenderContext) {
        when (accessory.type) {
            "icon" -> IconAccessoryRenderer.Render(accessory)
            "divider" -> DividerAccessoryRenderer.Render(accessory)
            "image" -> ImageAccessoryRenderer.Render(accessory, context)
            else -> Box {}
        }
    }
}

object IconAccessoryRenderer {
    @Composable
    fun Render(accessory: SduiAccessory) {
        val properties = accessory.properties
        val size = (properties.float("size") ?: 20f).dp
        val tint = properties.string("color")?.let(::parseSduiColor)
        val vector = when (properties.string("name")) {
            "arrow_forward" -> Icons.AutoMirrored.Filled.ArrowForward
            "edit" -> Icons.Default.Edit
            else -> null
        }
        if (vector != null) {
            Icon(
                imageVector = vector,
                contentDescription = null,
                modifier = Modifier.width(size).height(size),
                tint = tint ?: androidx.compose.ui.graphics.Color.Unspecified,
            )
        } else {
            Text(properties.string("name").orEmpty())
        }
    }
}

object DividerAccessoryRenderer {
    @Composable
    fun Render(accessory: SduiAccessory) {
        val properties = accessory.properties
        val thickness = (properties.float("thickness") ?: 1f).dp
        val color = properties.string("color")?.let(::parseSduiColor) ?: androidx.compose.material3.DividerDefaults.color
        if (properties.string("orientation") == "vertical") {
            VerticalDivider(
                modifier = Modifier.height((properties.float("height") ?: 24f).dp),
                thickness = thickness,
                color = color,
            )
        } else {
            HorizontalDivider(
                modifier = Modifier.width((properties.float("width") ?: 36f).dp),
                thickness = thickness,
                color = color,
            )
        }
    }
}

object ImageAccessoryRenderer {
    @Composable
    fun Render(accessory: SduiAccessory, context: SduiRenderContext) {
        val properties = accessory.properties
        val url = properties["url"].resolvedContent(context) ?: return
        AsyncImage(
            model = context.resolveAssetUrl(url),
            contentDescription = null,
            modifier = Modifier
                .then(properties.float("width")?.let { Modifier.width(it.dp) } ?: Modifier)
                .then(properties.float("height")?.let { Modifier.height(it.dp) } ?: Modifier),
            contentScale = ContentScale.Fit,
        )
    }
}
