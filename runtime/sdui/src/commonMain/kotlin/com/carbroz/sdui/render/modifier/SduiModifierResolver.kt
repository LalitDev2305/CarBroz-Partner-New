package com.carbroz.sdui.render.modifier

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.width
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.shape.RoundedCornerShape
import com.carbroz.sdui.render.arrayValue
import com.carbroz.sdui.render.boolean
import com.carbroz.sdui.render.float
import com.carbroz.sdui.render.objectValue
import com.carbroz.sdui.render.string
import kotlinx.serialization.json.JsonObject

fun Modifier.applySduiProperties(properties: JsonObject): Modifier {
    var result = this
    when {
        properties.boolean("fillMaxSize") == true -> result = result.fillMaxSize()
        else -> {
            if (properties.boolean("fillMaxWidth") == true) result = result.fillMaxWidth()
            if (properties.boolean("fillMaxHeight") == true) result = result.fillMaxHeight()
        }
    }
    properties.float("width")?.let { result = result.width(it.dp) }
    properties.float("height")?.let { result = result.height(it.dp) }
    if (listOf("minWidth", "maxWidth", "minHeight", "maxHeight").any(properties::containsKey)) {
        result = result.sizeIn(
            minWidth = properties.float("minWidth")?.dp ?: androidx.compose.ui.unit.Dp.Unspecified,
            maxWidth = properties.float("maxWidth")?.dp ?: androidx.compose.ui.unit.Dp.Unspecified,
            minHeight = properties.float("minHeight")?.dp ?: androidx.compose.ui.unit.Dp.Unspecified,
            maxHeight = properties.float("maxHeight")?.dp ?: androidx.compose.ui.unit.Dp.Unspecified,
        )
    }

    val shape = properties.sduiShape()
    if (shape != null) result = result.clip(shape)

    properties.objectValue("background")?.let { background ->
        val solid = background.string("color")?.let(::parseSduiColor)
        val gradient = background.sduiGradient()
        result = when {
            gradient != null -> result.background(gradient, shape ?: RoundedCornerShape(0.dp))
            solid != null -> result.background(solid, shape ?: RoundedCornerShape(0.dp))
            else -> result
        }
    }

    properties.objectValue("border")?.let { border ->
        val width = border.float("width") ?: 0f
        val color = border.string("color")?.let(::parseSduiColor)
        if (width > 0f && color != null) {
            result = result.border(width.dp, color, shape ?: RoundedCornerShape(0.dp))
        }
    }

    properties.objectValue("padding")?.let { padding ->
        result = result.padding(
            start = (padding.float("start") ?: 0f).dp,
            top = (padding.float("top") ?: 0f).dp,
            end = (padding.float("end") ?: 0f).dp,
            bottom = (padding.float("bottom") ?: 0f).dp,
        )
    }
    return result
}

internal fun JsonObject.sduiShape(): Shape? {
    val shape = objectValue("shape") ?: return null
    return when (shape.string("type")) {
        "roundedCorner" -> RoundedCornerShape((shape.float("cornerRadius") ?: 0f).dp)
        else -> null
    }
}

private fun JsonObject.sduiGradient(): Brush? {
    if (string("type") != "linearGradient") return null
    val colors = arrayValue("colors")
        ?.mapNotNull { item ->
            val stop = item as? JsonObject ?: return@mapNotNull null
            stop.string("color")?.let(::parseSduiColor)
        }
        .orEmpty()
    return colors.takeIf { it.size >= 2 }?.let(Brush::linearGradient)
}

fun parseSduiColor(value: String): Color? {
    val normalized = value.trim().removePrefix("#")
    val argb = when (normalized.length) {
        6 -> "FF$normalized"
        8 -> normalized
        else -> return null
    }
    return runCatching { Color(argb.toLong(16).toInt()) }.getOrNull()
}
