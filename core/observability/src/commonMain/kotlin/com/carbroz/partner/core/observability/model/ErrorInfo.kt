package com.carbroz.partner.core.observability.model

/**
 * Safe sanitized error representation preventing raw Throwable or stack traces from crossing sink boundaries.
 */
data class ErrorInfo(
    val type: String,
    val message: String?,
    val causeType: String? = null,
    val causeMessage: String? = null
)
