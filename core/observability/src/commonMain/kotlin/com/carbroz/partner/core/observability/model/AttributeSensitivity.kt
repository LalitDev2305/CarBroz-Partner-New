package com.carbroz.partner.core.observability.model

/**
 * Sensitivity classification for log attributes used by policy filtering.
 */
enum class AttributeSensitivity {
    /**
     * Standard public metadata safe for general production logging.
     */
    PUBLIC,

    /**
     * Detailed diagnostic information retained in verbose development logs.
     */
    DETAIL,

    /**
     * Raw payload structures filtered by default unless explicit payload policy is active.
     */
    PAYLOAD
}
