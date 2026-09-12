package com.carbroz.sdui.model

import kotlinx.serialization.Serializable

@Serializable
data class SduiDestination(
    val screenId: String,
    val templateId: String,
    val templateType: String,
    val endpoint: String,
    val method: SduiRequestMethod,
    val authentication: SduiAuthentication,
)
