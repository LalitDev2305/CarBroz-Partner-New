package com.carbroz.partner.sdui.render.renderer.childdata.timer

import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonPrimitive

public data class TimerChildrenDataProperties(
    val initialSeconds: Int = 60,
    val format: String = "mm:ss"
) {
    public companion object {
        public fun decode(jsonObject: JsonObject?): TimerChildrenDataProperties {
            if (jsonObject == null) return TimerChildrenDataProperties()
            val initSec = jsonObject["initial_seconds"]?.jsonPrimitive?.content?.toIntOrNull() ?: 60
            val fmt = jsonObject["format"]?.jsonPrimitive?.content ?: "mm:ss"
            return TimerChildrenDataProperties(initialSeconds = initSec, format = fmt)
        }
    }
}
