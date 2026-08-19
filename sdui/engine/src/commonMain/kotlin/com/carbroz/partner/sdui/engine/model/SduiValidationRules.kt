package com.carbroz.partner.sdui.engine.model

/**
 * Immutable validation rules model decoded from node properties in renderer layer.
 */
public data class SduiValidationRules(
    val required: Boolean = false,
    val regex: String? = null,
    val minLength: Int? = null,
    val maxLength: Int? = null,
    val errorMessage: String? = null
)
