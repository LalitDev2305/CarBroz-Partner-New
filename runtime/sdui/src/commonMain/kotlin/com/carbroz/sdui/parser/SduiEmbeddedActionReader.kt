package com.carbroz.sdui.parser

import com.carbroz.sdui.model.SduiAction
import com.carbroz.sdui.model.SduiElement
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject

internal data class SduiEmbeddedActionReadResult(
    val actions: List<SduiAction>,
    val invalid: Boolean,
)

/**
 * Owns discovery/decoding of actions embedded inside element property payloads.
 * Renderers own visual interpretation; the support checker only consumes this normalized action view.
 */
internal fun SduiElement.readEmbeddedActions(decoder: SduiDecoder): SduiEmbeddedActionReadResult {
    val rawActions = embeddedActionPayloads()
    val actions = ArrayList<SduiAction>(rawActions.size)
    rawActions.forEach { raw ->
        val action = decoder.decodeAction(raw)
            ?: return SduiEmbeddedActionReadResult(actions = emptyList(), invalid = true)
        actions += action
    }
    return SduiEmbeddedActionReadResult(actions = actions, invalid = false)
}

internal fun SduiElement.decodeTextSpanAction(index: Int, decoder: SduiDecoder): SduiAction? {
    val span = textSpans()?.getOrNull(index) as? JsonObject ?: return null
    val raw = span["onClick"] ?: return null
    return decoder.decodeAction(raw)
}

private fun SduiElement.embeddedActionPayloads(): List<JsonElement> = when (type) {
    "text" -> textSpans()
        ?.mapNotNull { item -> (item as? JsonObject)?.get("onClick") }
        .orEmpty()
    else -> emptyList()
}

private fun SduiElement.textSpans(): JsonArray? = properties["spans"] as? JsonArray
