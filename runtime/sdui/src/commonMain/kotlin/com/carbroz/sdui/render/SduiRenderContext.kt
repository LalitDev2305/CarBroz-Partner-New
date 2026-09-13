package com.carbroz.sdui.render

import com.carbroz.sdui.runtime.FieldState
import com.carbroz.sdui.runtime.NodeRuntimeState
import kotlinx.serialization.json.JsonElement

data class SduiRenderContext(
    val fields: Map<String, FieldState> = emptyMap(),
    val nodeStates: Map<String, NodeRuntimeState> = emptyMap(),
    val resolveValue: (JsonElement) -> JsonElement? = { it },
    val resolveAssetUrl: (String) -> String = { it },
    val onInteraction: (SduiInteraction) -> Unit,
)
