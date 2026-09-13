package com.carbroz.feature.dynamic

import com.carbroz.sdui.model.SduiScreen
import com.carbroz.sdui.runtime.FieldState
import com.carbroz.sdui.runtime.NodeRuntimeState
import com.carbroz.sdui.runtime.SduiOverlay
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject

/** Sole observable mutable state for one live dynamic screen. */
data class DynamicScreenState(
    val destination: DynamicDestination? = null,
    val loading: Boolean = false,
    val actionInFlight: Boolean = false,
    val screen: SduiScreen? = null,
    val fields: Map<String, FieldState> = emptyMap(),
    val nodeStates: Map<String, NodeRuntimeState> = emptyMap(),
    val overlay: SduiOverlay? = null,
    val context: JsonObject = JsonObject(emptyMap()),
    val response: JsonElement? = null,
    val failure: DynamicScreenFailure? = null,
)

sealed interface DynamicScreenFailure {
    data class Network(val code: String) : DynamicScreenFailure
    data class Decode(val reason: String) : DynamicScreenFailure
    data class UnsupportedContract(val reason: String) : DynamicScreenFailure
    data class Action(val reason: String) : DynamicScreenFailure
}
