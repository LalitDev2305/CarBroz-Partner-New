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

@Composable
internal fun RenderStack(
    properties: JsonObject,
    modifier: Modifier = Modifier,
    children: @Composable () -> Unit,
) {
    val resolvedModifier = modifier.applySduiProperties(properties)
    val spacing = properties.float("spacing") ?: 16f
    val mainAxisAlignment = properties.string("mainAxisAlignment")
    val crossAxisAlignment = properties.string("crossAxisAlignment")

    when (properties.string("axis")?.uppercase() ?: "VERTICAL") {
        "HORIZONTAL" -> Row(
            modifier = resolvedModifier,
            horizontalArrangement = horizontalArrangement(spacing, mainAxisAlignment),
            verticalAlignment = verticalAlignment(crossAxisAlignment),
            content = { children() },
        )
        else -> Column(
            modifier = resolvedModifier,
            verticalArrangement = verticalArrangement(spacing, mainAxisAlignment),
            horizontalAlignment = horizontalAlignment(crossAxisAlignment),
            content = { children() },
        )
    }
}

private fun verticalArrangement(spacing: Float, value: String?): Arrangement.Vertical =
    if (spacing > 0f) Arrangement.spacedBy(spacing.dp) else when (value.normalizedAlignment()) {
        "CENTER" -> Arrangement.Center
        "END" -> Arrangement.Bottom
        "SPACE_BETWEEN" -> Arrangement.SpaceBetween
        "SPACE_AROUND" -> Arrangement.SpaceAround
        "SPACE_EVENLY" -> Arrangement.SpaceEvenly
        else -> Arrangement.Top
    }

private fun horizontalArrangement(spacing: Float, value: String?): Arrangement.Horizontal =
    if (spacing > 0f) Arrangement.spacedBy(spacing.dp) else when (value.normalizedAlignment()) {
        "CENTER" -> Arrangement.Center
        "END" -> Arrangement.End
        "SPACE_BETWEEN" -> Arrangement.SpaceBetween
        "SPACE_AROUND" -> Arrangement.SpaceAround
        "SPACE_EVENLY" -> Arrangement.SpaceEvenly
        else -> Arrangement.Start
    }

private fun horizontalAlignment(value: String?): Alignment.Horizontal = when (value.normalizedAlignment()) {
    "CENTER" -> Alignment.CenterHorizontally
    "END" -> Alignment.End
    else -> Alignment.Start
}

private fun verticalAlignment(value: String?): Alignment.Vertical = when (value.normalizedAlignment()) {
    "CENTER" -> Alignment.CenterVertically
    "END" -> Alignment.Bottom
    else -> Alignment.Top
}

private fun String?.normalizedAlignment(): String? = this
    ?.replace("-", "_")
    ?.replace(" ", "_")
    ?.let { raw ->
        buildString {
            raw.forEachIndexed { index, char ->
                if (char.isUpperCase() && index > 0 && raw[index - 1].isLowerCase()) append('_')
                append(char.uppercaseChar())
            }
        }
    }
