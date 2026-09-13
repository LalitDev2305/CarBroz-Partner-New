package com.carbroz.sdui.model

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

@Serializable
data class SduiScreen(
    val screenId: String,
    val schemaVersion: String,
    val targetApp: SduiTargetApp,
    val template: SduiTemplate,
    val theme: SduiTheme? = null,
    val metadata: JsonObject? = null,
)

@Serializable
enum class SduiTargetApp {
    GLOBAL,
    PARTNER,
    CUSTOMER,
}
