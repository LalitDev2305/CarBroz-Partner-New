package com.carbroz.partner.sdui.render.renderer.childdata.button

import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonPrimitive

public data class ButtonChildrenDataProperties(
    val text: String = "",
    val styleKey: String? = null
) {
    public companion object {
        public fun decode(jsonObject: JsonObject?): ButtonChildrenDataProperties {
            if (jsonObject == null) return ButtonChildrenDataProperties()
            val textVal = jsonObject["text"]?.jsonPrimitive?.content ?: ""
            val styleVal = jsonObject["style"]?.jsonPrimitive?.content
            return ButtonChildrenDataProperties(text = textVal, styleKey = styleVal)
        }
    }
}
