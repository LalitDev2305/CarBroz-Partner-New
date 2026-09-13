package com.carbroz.sdui.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

@Serializable
data class SduiTheme(
    val theme: SduiThemeMode? = null,
    val showBackButton: Boolean? = null,
    val statusBar: SduiStatusBarMode? = null,
    val properties: JsonObject? = null,
)

@Serializable
enum class SduiThemeMode {
    @SerialName("light") LIGHT,
    @SerialName("dark") DARK,
}

@Serializable
enum class SduiStatusBarMode {
    @SerialName("transparent") TRANSPARENT,
    @SerialName("default") DEFAULT,
}
