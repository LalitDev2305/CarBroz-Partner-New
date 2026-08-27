package com.carbroz.runtime.sdui.properties

import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.floatOrNull

data class EdgeInsetsDp(
    val start: Float = 0f,
    val top: Float = 0f,
    val end: Float = 0f,
    val bottom: Float = 0f,
) {
    companion object { val Zero = EdgeInsetsDp() }
}

data class CommonNodeProperties(
    val visible: Boolean = true,
    val fillWidth: Boolean = false,
    val fillHeight: Boolean = false,
    val widthDp: Float? = null,
    val heightDp: Float? = null,
    val minWidthDp: Float? = null,
    val maxWidthDp: Float? = null,
    val minHeightDp: Float? = null,
    val maxHeightDp: Float? = null,
    val padding: EdgeInsetsDp = EdgeInsetsDp.Zero,
    val margin: EdgeInsetsDp = EdgeInsetsDp.Zero,
    val backgroundColor: String? = null,
    val borderColor: String? = null,
    val borderWidthDp: Float = 0f,
    val cornerRadiusDp: Float = 0f,
)

sealed interface CommonPropertiesDecodeResult {
    data class Success(val value: CommonNodeProperties) : CommonPropertiesDecodeResult
    data class Failure(val reason: String) : CommonPropertiesDecodeResult
}

/** Decodes only properties shared safely across hierarchy levels. Type-specific decoders own everything else. */
object CommonNodePropertiesDecoder {
    fun decode(raw: JsonObject): CommonPropertiesDecodeResult {
        fun number(name: String): Float? {
            val element = raw[name] ?: return null
            val value = (element as? JsonPrimitive)?.floatOrNull
                ?: throw InvalidCommonProperty("'$name' must be a number")
            if (value < 0f) throw InvalidCommonProperty("'$name' must be non-negative")
            return value
        }

        fun boolean(name: String, default: Boolean): Boolean {
            val element = raw[name] ?: return default
            return (element as? JsonPrimitive)?.booleanOrNull
                ?: throw InvalidCommonProperty("'$name' must be a boolean")
        }

        fun color(name: String): String? {
            val element = raw[name] ?: return null
            val value = (element as? JsonPrimitive)?.contentOrNull?.trim()
                ?: throw InvalidCommonProperty("'$name' must be a string")
            if (!HEX_COLOR.matches(value)) throw InvalidCommonProperty("'$name' must be #RRGGBB or #AARRGGBB")
            return value.uppercase()
        }

        fun insets(name: String): EdgeInsetsDp {
            val element = raw[name] ?: return EdgeInsetsDp.Zero
            return decodeInsets(name, element)
        }

        return try {
            val minWidth = number("minWidth")
            val maxWidth = number("maxWidth")
            val minHeight = number("minHeight")
            val maxHeight = number("maxHeight")
            if (minWidth != null && maxWidth != null && minWidth > maxWidth) {
                throw InvalidCommonProperty("'minWidth' must be <= 'maxWidth'")
            }
            if (minHeight != null && maxHeight != null && minHeight > maxHeight) {
                throw InvalidCommonProperty("'minHeight' must be <= 'maxHeight'")
            }
            CommonPropertiesDecodeResult.Success(
                CommonNodeProperties(
                    visible = boolean("visible", true),
                    fillWidth = boolean("fillWidth", false),
                    fillHeight = boolean("fillHeight", false),
                    widthDp = number("width"),
                    heightDp = number("height"),
                    minWidthDp = minWidth,
                    maxWidthDp = maxWidth,
                    minHeightDp = minHeight,
                    maxHeightDp = maxHeight,
                    padding = insets("padding"),
                    margin = insets("margin"),
                    backgroundColor = color("backgroundColor"),
                    borderColor = color("borderColor"),
                    borderWidthDp = number("borderWidth") ?: 0f,
                    cornerRadiusDp = number("cornerRadius") ?: 0f,
                ),
            )
        } catch (failure: InvalidCommonProperty) {
            CommonPropertiesDecodeResult.Failure(failure.message ?: "invalid common property")
        }
    }

    private fun decodeInsets(name: String, element: JsonElement): EdgeInsetsDp {
        if (element is JsonPrimitive) {
            val all = element.floatOrNull ?: throw InvalidCommonProperty("'$name' must be a number or object")
            if (all < 0f) throw InvalidCommonProperty("'$name' must be non-negative")
            return EdgeInsetsDp(all, all, all, all)
        }
        val objectValue = element as? JsonObject ?: throw InvalidCommonProperty("'$name' must be a number or object")
        fun side(key: String): Float {
            val value = objectValue[key] ?: return 0f
            val number = (value as? JsonPrimitive)?.floatOrNull
                ?: throw InvalidCommonProperty("'$name.$key' must be a number")
            if (number < 0f) throw InvalidCommonProperty("'$name.$key' must be non-negative")
            return number
        }
        val horizontal = side("horizontal")
        val vertical = side("vertical")
        return EdgeInsetsDp(
            start = objectValue["start"]?.let { side("start") } ?: horizontal,
            top = objectValue["top"]?.let { side("top") } ?: vertical,
            end = objectValue["end"]?.let { side("end") } ?: horizontal,
            bottom = objectValue["bottom"]?.let { side("bottom") } ?: vertical,
        )
    }

    private class InvalidCommonProperty(message: String) : RuntimeException(message)
    private val HEX_COLOR = Regex("^#(?:[0-9A-Fa-f]{6}|[0-9A-Fa-f]{8})$")
}
