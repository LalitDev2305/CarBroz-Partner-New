package com.carbroz.runtime.sdui.rendering

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.carbroz.runtime.sdui.properties.CommonNodeProperties
import com.carbroz.runtime.sdui.properties.EdgeInsetsDp

fun Modifier.applyCommonNodeProperties(properties: CommonNodeProperties): Modifier {
    if (!properties.visible) return this
    var result = this.padding(properties.margin.toPaddingValues())
    if (properties.fillWidth) result = result.fillMaxWidth()
    if (properties.fillHeight) result = result.fillMaxHeight()
    properties.widthDp?.let { result = result.width(it.dp) }
    properties.heightDp?.let { result = result.height(it.dp) }
    if (
        properties.minWidthDp != null || properties.maxWidthDp != null ||
        properties.minHeightDp != null || properties.maxHeightDp != null
    ) {
        result = result.sizeIn(
            minWidth = properties.minWidthDp?.dp ?: Dp.Unspecified,
            maxWidth = properties.maxWidthDp?.dp ?: Dp.Unspecified,
            minHeight = properties.minHeightDp?.dp ?: Dp.Unspecified,
            maxHeight = properties.maxHeightDp?.dp ?: Dp.Unspecified,
        )
    }
    val shape = RoundedCornerShape(properties.cornerRadiusDp.dp)
    if (properties.cornerRadiusDp > 0f) result = result.clip(shape)
    properties.backgroundColor?.let { result = result.background(parseHexColor(it), shape) }
    if (properties.borderWidthDp > 0f && properties.borderColor != null) {
        result = result.border(properties.borderWidthDp.dp, parseHexColor(properties.borderColor), shape)
    }
    return result.padding(properties.padding.toPaddingValues())
}

private fun EdgeInsetsDp.toPaddingValues() = PaddingValues(
    start = start.dp,
    top = top.dp,
    end = end.dp,
    bottom = bottom.dp,
)

private fun parseHexColor(value: String): Color {
    val hex = value.removePrefix("#")
    val argb = if (hex.length == 6) "FF$hex" else hex
    return Color(argb.toLong(16).toInt())
}
