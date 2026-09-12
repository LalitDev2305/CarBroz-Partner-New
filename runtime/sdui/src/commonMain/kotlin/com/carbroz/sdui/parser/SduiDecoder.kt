package com.carbroz.sdui.parser

import com.carbroz.sdui.model.SduiAction
import com.carbroz.sdui.model.SduiAccessory
import com.carbroz.sdui.model.SduiScreen
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.decodeFromJsonElement

class SduiDecoder(
    private val json: Json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
        classDiscriminator = "type"
    },
) {
    fun decode(element: JsonElement): SduiDecodeResult =
        runCatching { json.decodeFromJsonElement<SduiScreen>(element) }
            .fold(
                onSuccess = { SduiDecodeResult.Success(it) },
                onFailure = { SduiDecodeResult.Failure(it.message ?: "sdui_decode_failure") },
            )

    fun decodeAction(element: JsonElement): SduiAction? =
        runCatching { json.decodeFromJsonElement<SduiAction>(element) }.getOrNull()

    fun decodeAccessory(element: JsonElement): SduiAccessory? =
        runCatching { json.decodeFromJsonElement<SduiAccessory>(element) }.getOrNull()

    internal fun decodeAction(text: String): SduiAction? =
        runCatching { json.decodeFromString<SduiAction>(text) }.getOrNull()
}
