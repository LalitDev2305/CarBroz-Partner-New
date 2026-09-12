package com.carbroz.sdui.render

import com.carbroz.sdui.model.SduiAction
import kotlinx.serialization.json.JsonElement

sealed interface SduiInteraction {
    data class ValueChanged(
        val elementId: String,
        val bindingKey: String,
        val value: JsonElement,
    ) : SduiInteraction

    data class ActionTriggered(
        val sourceId: String,
        val event: String,
        val action: SduiAction,
    ) : SduiInteraction
}
