package com.carbroz.sdui.render

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.carbroz.sdui.render.modifier.applySduiProperties
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull

@Composable
internal fun RenderStack(
    properties: JsonObject,
    modifier: Modifier = Modifier,
    children: @Composable () -> Unit,
) {
    val resolvedModifier = modifier.applySduiProperties(properties)
    when (properties.string("orientation") ?: "vertical") {
        "horizontal" -> Row(
            modifier = resolvedModifier,
            horizontalArrangement = horizontalArrangement(properties["horizontalArrangement"]),
            verticalAlignment = verticalAlignment(properties.string("verticalAlignment")),
            content = { children() },
        )
        else -> Column(
            modifier = resolvedModifier,
            verticalArrangement = verticalArrangement(properties["verticalArrangement"]),
            horizontalAlignment = horizontalAlignment(properties.string("horizontalAlignment")),
            content = { children() },
        )
    }
}

private fun verticalArrangement(value: kotlinx.serialization.json.JsonElement?): Arrangement.Vertical =
    when (value) {
        is JsonPrimitive -> when (value.contentOrNull) {
            "center" -> Arrangement.Center
            "end" -> Arrangement.Bottom
            "spaceBetween" -> Arrangement.SpaceBetween
            "spaceAround" -> Arrangement.SpaceAround
            "spaceEvenly" -> Arrangement.SpaceEvenly
            else -> Arrangement.Top
        }
        is JsonObject -> if (value.string("type") == "spacedBy") {
            Arrangement.spacedBy((value.float("spacing") ?: 0f).dp)
        } else Arrangement.Top
        else -> Arrangement.Top
    }

private fun horizontalArrangement(value: kotlinx.serialization.json.JsonElement?): Arrangement.Horizontal =
    when (value) {
        is JsonPrimitive -> when (value.contentOrNull) {
            "center" -> Arrangement.Center
            "end" -> Arrangement.End
            "spaceBetween" -> Arrangement.SpaceBetween
            "spaceAround" -> Arrangement.SpaceAround
            "spaceEvenly" -> Arrangement.SpaceEvenly
            else -> Arrangement.Start
        }
        is JsonObject -> if (value.string("type") == "spacedBy") {
            Arrangement.spacedBy((value.float("spacing") ?: 0f).dp)
        } else Arrangement.Start
        else -> Arrangement.Start
    }

private fun horizontalAlignment(value: String?): Alignment.Horizontal = when (value) {
    "center" -> Alignment.CenterHorizontally
    "end" -> Alignment.End
    else -> Alignment.Start
}

private fun verticalAlignment(value: String?): Alignment.Vertical = when (value) {
    "center" -> Alignment.CenterVertically
    "bottom" -> Alignment.Bottom
    else -> Alignment.Top
}
