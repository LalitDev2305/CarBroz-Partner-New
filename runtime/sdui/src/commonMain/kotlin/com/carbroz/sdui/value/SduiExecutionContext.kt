package com.carbroz.sdui.value

import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject

data class SduiExecutionContext(
    val bindings: Map<String, JsonElement> = emptyMap(),
    val context: JsonObject = JsonObject(emptyMap()),
    val response: JsonElement? = null,
)
