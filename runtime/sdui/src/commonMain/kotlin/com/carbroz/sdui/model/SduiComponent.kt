package com.carbroz.sdui.model

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

@Serializable
data class SduiComponent(
    val id: String,
    val type: String,
    val properties: JsonObject = JsonObject(emptyMap()),
    val elements: List<SduiElement>? = null,
    val sections: List<SduiSection>? = null,
)
