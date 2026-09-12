package com.carbroz.sdui.model

import kotlinx.serialization.Serializable

@Serializable
data class SduiValidationRule(
    val required: Boolean = false,
    val pattern: String? = null,
    val message: String? = null,
)
