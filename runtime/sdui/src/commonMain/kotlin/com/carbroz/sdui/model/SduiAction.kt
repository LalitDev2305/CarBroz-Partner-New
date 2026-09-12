package com.carbroz.sdui.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject

@Serializable
sealed interface SduiAction {
    @Serializable
    @SerialName("request")
    data class Request(val payload: RequestPayload) : SduiAction

    @Serializable
    @SerialName("navigate")
    data class Navigate(
        val payload: SduiDestination,
        val navigationMode: SduiNavigationMode = SduiNavigationMode.PUSH,
    ) : SduiAction

    @Serializable
    @SerialName("present")
    data class Present(
        val targetId: String,
        val payload: PresentPayload,
    ) : SduiAction

    @Serializable
    @SerialName("dismiss")
    data class Dismiss(val targetId: String? = null) : SduiAction

    @Serializable
    @SerialName("state")
    data class State(
        val targetId: String,
        val payload: StatePayload,
    ) : SduiAction

    @Serializable
    @SerialName("external_uri")
    data class ExternalUri(val payload: ExternalUriPayload) : SduiAction

    @Serializable
    @SerialName("sequence")
    data class Sequence(val payload: SequencePayload) : SduiAction
}

@Serializable
data class RequestPayload(
    val method: SduiRequestMethod,
    val endpoint: String,
    val authentication: SduiAuthentication,
    val validate: Boolean = false,
    val body: JsonObject? = null,
    val responseMode: SduiRequestResponseMode = SduiRequestResponseMode.NONE,
    val navigationMode: SduiNavigationMode = SduiNavigationMode.PUSH,
    val contextUpdates: JsonObject? = null,
)

@Serializable
data class PresentPayload(val presentation: SduiPresentationMode)

@Serializable
data class StatePayload(
    val operation: SduiStateOperation,
    val property: SduiStateProperty,
    val value: JsonElement? = null,
)

@Serializable
data class ExternalUriPayload(val uri: JsonElement)

@Serializable
data class SequencePayload(val actions: List<SduiAction>)

@Serializable
enum class SduiRequestMethod { GET, POST, PUT, PATCH, DELETE }

@Serializable
enum class SduiAuthentication { NONE, SESSION }

@Serializable
enum class SduiRequestResponseMode {
    @SerialName("none") NONE,
    @SerialName("destination") DESTINATION,
}

@Serializable
enum class SduiNavigationMode {
    @SerialName("push") PUSH,
    @SerialName("replace") REPLACE,
    @SerialName("reset") RESET,
}

@Serializable
enum class SduiPresentationMode {
    @SerialName("dialog") DIALOG,
    @SerialName("bottom_sheet") BOTTOM_SHEET,
    @SerialName("popup") POPUP,
}

@Serializable
enum class SduiStateOperation {
    @SerialName("set") SET,
    @SerialName("toggle") TOGGLE,
}

@Serializable
enum class SduiStateProperty {
    @SerialName("visible") VISIBLE,
    @SerialName("enabled") ENABLED,
    @SerialName("selected") SELECTED,
    @SerialName("expanded") EXPANDED,
    @SerialName("checked") CHECKED,
    @SerialName("loading") LOADING,
    @SerialName("value") VALUE,
}
