package com.carbroz.partner.sdui.render.renderer.childdata.input

import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonPrimitive

public data class InputChildrenDataProperties(
    val placeholder: String = "",
    val label: String = ""
) {
    public companion object {
        public fun decode(jsonObject: JsonObject?): InputChildrenDataProperties {
            if (jsonObject == null) return InputChildrenDataProperties()
            val placeholderVal = jsonObject["placeholder"]?.jsonPrimitive?.content ?: ""
            val labelVal = jsonObject["label"]?.jsonPrimitive?.content ?: ""
            return InputChildrenDataProperties(placeholder = placeholderVal, label = labelVal)
        }
    }
}
