package com.carbroz.sdui.model

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

@Serializable
data class SduiElement(
    val id: String,
    val type: String,
    val properties: JsonObject = JsonObject(emptyMap()),
    val actions: Map<String, SduiAction> = emptyMap(),
    val analytics: JsonObject? = null,
    val accessibility: JsonObject? = null,
    val validation: SduiValidationRule? = null,
    val binding: SduiElementBinding? = null,
    val visibility: JsonObject? = null,
    val metadata: JsonObject? = null,
)

@Serializable
data class SduiElementBinding(
    val key: String,
)
