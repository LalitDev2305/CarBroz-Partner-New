package com.carbroz.runtime.sdui.properties

import com.carbroz.runtime.sdui.extension.PropertyDecodeResult
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.floatOrNull

enum class StackAxis { VERTICAL, HORIZONTAL }
enum class StackMainAxisAlignment { START, CENTER, END, SPACE_BETWEEN, SPACE_AROUND, SPACE_EVENLY }
enum class StackCrossAxisAlignment { START, CENTER, END, STRETCH }

data class StackContainerProperties(
    override val common: CommonNodeProperties,
    val axis: StackAxis = StackAxis.VERTICAL,
    val spacingDp: Float = 16f,
    val mainAxisAlignment: StackMainAxisAlignment = StackMainAxisAlignment.START,
    val crossAxisAlignment: StackCrossAxisAlignment = StackCrossAxisAlignment.START,
) : CommonNodePropertyOwner

object StackContainerPropertiesDecoder {
    fun decode(raw: JsonObject): PropertyDecodeResult<StackContainerProperties> {
        val common = when (
            val result = CommonNodePropertiesDecoder.decode(
                raw,
                defaults = CommonNodeProperties(fillWidth = true),
            )
        ) {
            is CommonPropertiesDecodeResult.Success -> result.value
            is CommonPropertiesDecodeResult.Failure -> return PropertyDecodeResult.Failure(result.reason)
        }
        val axis = enumValue<StackAxis>(raw, "axis", StackAxis.VERTICAL)
            ?: return PropertyDecodeResult.Failure("STACK 'axis' is unsupported")
        val main = enumValue<StackMainAxisAlignment>(raw, "mainAxisAlignment", StackMainAxisAlignment.START)
            ?: return PropertyDecodeResult.Failure("STACK 'mainAxisAlignment' is unsupported")
        val cross = enumValue<StackCrossAxisAlignment>(raw, "crossAxisAlignment", StackCrossAxisAlignment.START)
            ?: return PropertyDecodeResult.Failure("STACK 'crossAxisAlignment' is unsupported")
        val spacing = (raw["spacing"] as? JsonPrimitive)?.floatOrNull ?: 16f
        if (spacing < 0f) return PropertyDecodeResult.Failure("STACK 'spacing' must be non-negative")
        return PropertyDecodeResult.Success(StackContainerProperties(common, axis, spacing, main, cross))
    }

    private inline fun <reified T : Enum<T>> enumValue(raw: JsonObject, key: String, default: T): T? {
        val value = (raw[key] as? JsonPrimitive)?.contentOrNull ?: return default
        return enumValues<T>().firstOrNull { it.name == value.uppercase() }
    }
}
