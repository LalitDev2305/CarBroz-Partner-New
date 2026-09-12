package com.carbroz.sdui.model

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

@Serializable
data class SduiAccessory(
    val type: String,
    val properties: JsonObject = JsonObject(emptyMap()),
)
