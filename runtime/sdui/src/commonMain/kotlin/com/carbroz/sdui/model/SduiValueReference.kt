package com.carbroz.sdui.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

@Serializable
data class SduiValueReference(
    @SerialName("\$binding") val binding: String? = null,
    @SerialName("\$context") val context: String? = null,
    @SerialName("\$response") val response: String? = null,
    @SerialName("\$literal") val literal: JsonElement? = null,
) {
    fun kindCount(): Int = listOf(binding, context, response, literal).count { it != null }
}
