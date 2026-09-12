package com.carbroz.sdui.render

import com.carbroz.sdui.model.SduiAccessory
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.floatOrNull
import kotlinx.serialization.json.intOrNull

internal fun JsonObject.string(key: String): String? = (this[key] as? JsonPrimitive)?.contentOrNull
internal fun JsonObject.float(key: String): Float? = (this[key] as? JsonPrimitive)?.floatOrNull
internal fun JsonObject.int(key: String): Int? = (this[key] as? JsonPrimitive)?.intOrNull
internal fun JsonObject.boolean(key: String): Boolean? = (this[key] as? JsonPrimitive)?.booleanOrNull
internal fun JsonObject.objectValue(key: String): JsonObject? = this[key] as? JsonObject
internal fun JsonObject.arrayValue(key: String): JsonArray? = this[key] as? JsonArray

internal fun JsonObject.accessories(key: String): List<SduiAccessory> {
    val value = this[key] ?: return emptyList()
    val values = if (value is JsonArray) value else JsonArray(listOf(value))
    return values.mapNotNull { item ->
        val objectValue = item as? JsonObject ?: return@mapNotNull null
        val type = objectValue.string("type") ?: return@mapNotNull null
        SduiAccessory(type = type, properties = objectValue.objectValue("properties") ?: JsonObject(emptyMap()))
    }
}

internal fun JsonElement?.resolvedContent(context: SduiRenderContext): String? {
    val resolved = this?.let(context.resolveValue) ?: return null
    return (resolved as? JsonPrimitive)?.contentOrNull
}
