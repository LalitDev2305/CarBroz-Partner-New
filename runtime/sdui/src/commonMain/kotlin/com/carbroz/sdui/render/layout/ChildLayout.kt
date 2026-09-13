package com.carbroz.sdui.render.layout

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.carbroz.sdui.render.float
import com.carbroz.sdui.render.string
import kotlinx.serialization.json.JsonObject

internal enum class LinearAxis {
    VERTICAL,
    HORIZONTAL,
}

internal enum class LinearMainAxisAlignment {
    START,
    CENTER,
    END,
    SPACE_BETWEEN,
    SPACE_AROUND,
    SPACE_EVENLY,
}

internal enum class LinearCrossAxisAlignment {
    START,
    CENTER,
    END,
}

internal data class LinearChildLayoutSpec(
    val axis: LinearAxis = LinearAxis.VERTICAL,
    val spacing: Float = 16f,
    val mainAxisAlignment: LinearMainAxisAlignment = LinearMainAxisAlignment.START,
    val crossAxisAlignment: LinearCrossAxisAlignment = LinearCrossAxisAlignment.START,
)

private val supportedAxes = LinearAxis.entries.mapTo(hashSetOf()) { it.name }
private val supportedMainAxisAlignments = LinearMainAxisAlignment.entries.mapTo(hashSetOf()) { it.name }
private val supportedCrossAxisAlignments = LinearCrossAxisAlignment.entries.mapTo(hashSetOf()) { it.name }

internal fun linearChildLayoutSupportError(properties: JsonObject): String? {
    properties.string("axis")?.let { raw ->
        if (raw.normalizedLayoutToken() !in supportedAxes) return "unsupported_layout_axis:$raw"
    }
    properties.string("mainAxisAlignment")?.let { raw ->
        if (raw.normalizedLayoutToken() !in supportedMainAxisAlignments) {
            return "unsupported_main_axis_alignment:$raw"
        }
    }
    properties.string("crossAxisAlignment")?.let { raw ->
        if (raw.normalizedLayoutToken() !in supportedCrossAxisAlignments) {
            return "unsupported_cross_axis_alignment:$raw"
        }
    }
    if ("spacing" in properties) {
        val spacing = properties.float("spacing") ?: return "unsupported_layout_spacing"
        if (!spacing.isFinite() || spacing < 0f) return "unsupported_layout_spacing:$spacing"
    }
    return null
}

internal fun resolveLinearChildLayout(properties: JsonObject): LinearChildLayoutSpec = LinearChildLayoutSpec(
    axis = when (properties.string("axis").normalizedLayoutToken()) {
        "HORIZONTAL" -> LinearAxis.HORIZONTAL
        else -> LinearAxis.VERTICAL
    },
    spacing = properties.float("spacing") ?: 16f,
    mainAxisAlignment = when (properties.string("mainAxisAlignment").normalizedLayoutToken()) {
        "CENTER" -> LinearMainAxisAlignment.CENTER
        "END" -> LinearMainAxisAlignment.END
        "SPACE_BETWEEN" -> LinearMainAxisAlignment.SPACE_BETWEEN
        "SPACE_AROUND" -> LinearMainAxisAlignment.SPACE_AROUND
        "SPACE_EVENLY" -> LinearMainAxisAlignment.SPACE_EVENLY
        else -> LinearMainAxisAlignment.START
    },
    crossAxisAlignment = when (properties.string("crossAxisAlignment").normalizedLayoutToken()) {
        "CENTER" -> LinearCrossAxisAlignment.CENTER
        "END" -> LinearCrossAxisAlignment.END
        else -> LinearCrossAxisAlignment.START
    },
)

@Composable
internal fun RenderChildLayout(
    properties: JsonObject,
    modifier: Modifier,
    children: @Composable () -> Unit,
) {
    val spec = resolveLinearChildLayout(properties)
    when (spec.axis) {
        LinearAxis.HORIZONTAL -> Row(
            modifier = modifier,
            horizontalArrangement = horizontalArrangement(spec),
            verticalAlignment = verticalAlignment(spec.crossAxisAlignment),
            content = { children() },
        )

        LinearAxis.VERTICAL -> Column(
            modifier = modifier,
            verticalArrangement = verticalArrangement(spec),
            horizontalAlignment = horizontalAlignment(spec.crossAxisAlignment),
            content = { children() },
        )
    }
}

private fun verticalArrangement(spec: LinearChildLayoutSpec): Arrangement.Vertical =
    when (spec.mainAxisAlignment) {
        LinearMainAxisAlignment.SPACE_BETWEEN -> Arrangement.SpaceBetween
        LinearMainAxisAlignment.SPACE_AROUND -> Arrangement.SpaceAround
        LinearMainAxisAlignment.SPACE_EVENLY -> Arrangement.SpaceEvenly
        LinearMainAxisAlignment.CENTER -> if (spec.spacing == 0f) {
            Arrangement.Center
        } else {
            Arrangement.spacedBy(spec.spacing.dp, Alignment.CenterVertically)
        }
        LinearMainAxisAlignment.END -> if (spec.spacing == 0f) {
            Arrangement.Bottom
        } else {
            Arrangement.spacedBy(spec.spacing.dp, Alignment.Bottom)
        }
        LinearMainAxisAlignment.START -> if (spec.spacing == 0f) {
            Arrangement.Top
        } else {
            Arrangement.spacedBy(spec.spacing.dp, Alignment.Top)
        }
    }

private fun horizontalArrangement(spec: LinearChildLayoutSpec): Arrangement.Horizontal =
    when (spec.mainAxisAlignment) {
        LinearMainAxisAlignment.SPACE_BETWEEN -> Arrangement.SpaceBetween
        LinearMainAxisAlignment.SPACE_AROUND -> Arrangement.SpaceAround
        LinearMainAxisAlignment.SPACE_EVENLY -> Arrangement.SpaceEvenly
        LinearMainAxisAlignment.CENTER -> if (spec.spacing == 0f) {
            Arrangement.Center
        } else {
            Arrangement.spacedBy(spec.spacing.dp, Alignment.CenterHorizontally)
        }
        LinearMainAxisAlignment.END -> if (spec.spacing == 0f) {
            Arrangement.End
        } else {
            Arrangement.spacedBy(spec.spacing.dp, Alignment.End)
        }
        LinearMainAxisAlignment.START -> if (spec.spacing == 0f) {
            Arrangement.Start
        } else {
            Arrangement.spacedBy(spec.spacing.dp, Alignment.Start)
        }
    }

private fun horizontalAlignment(value: LinearCrossAxisAlignment): Alignment.Horizontal = when (value) {
    LinearCrossAxisAlignment.CENTER -> Alignment.CenterHorizontally
    LinearCrossAxisAlignment.END -> Alignment.End
    LinearCrossAxisAlignment.START -> Alignment.Start
}

private fun verticalAlignment(value: LinearCrossAxisAlignment): Alignment.Vertical = when (value) {
    LinearCrossAxisAlignment.CENTER -> Alignment.CenterVertically
    LinearCrossAxisAlignment.END -> Alignment.Bottom
    LinearCrossAxisAlignment.START -> Alignment.Top
}

internal fun String?.normalizedLayoutToken(): String? = this
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
