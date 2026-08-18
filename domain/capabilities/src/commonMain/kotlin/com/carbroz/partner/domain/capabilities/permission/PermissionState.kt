package com.carbroz.partner.domain.capabilities.permission

/**
 * Cross-platform authorization states.
 */
enum class PermissionState {
    NOT_DETERMINED,
    GRANTED,
    DENIED,
    SETTINGS_RECOVERY_REQUIRED,
    UNSUPPORTED
}
