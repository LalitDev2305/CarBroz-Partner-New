package com.carbroz.sdui.runtime

import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive

data class FieldState(
    val value: JsonElement = JsonPrimitive(""),
    val error: String? = null,
    val touched: Boolean = false,
)
