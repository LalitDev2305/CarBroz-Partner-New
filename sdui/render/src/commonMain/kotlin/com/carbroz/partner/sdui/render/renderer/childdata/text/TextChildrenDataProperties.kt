package com.carbroz.partner.sdui.render.renderer.childdata.text

import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonPrimitive

public data class TextChildrenDataProperties(
    val text: String = "",
    val styleKey: String? = null,
    val colorString: String? = null
) {
    public companion object {
        public fun decode(jsonObject: JsonObject?): TextChildrenDataProperties {
            if (jsonObject == null) return TextChildrenDataProperties()
            val textVal = jsonObject["text"]?.jsonPrimitive?.content ?: ""
            val styleVal = jsonObject["style"]?.jsonPrimitive?.content
                ?: jsonObject["typography"]?.jsonPrimitive?.content
            val colorVal = jsonObject["color"]?.jsonPrimitive?.content
            return TextChildrenDataProperties(
                text = textVal,
                styleKey = styleVal,
                colorString = colorVal
            )
        }
    }
}
