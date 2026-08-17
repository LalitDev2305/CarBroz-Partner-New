package com.carbroz.partner.core.observability.model

/**
 * Closed semantic taxonomy for diagnostic log domain categories.
 * Section 14 & 18 of the Engineering Constitution mandate single-owner category governance.
 */
enum class LogCategory {
    APP,
    LIFECYCLE,
    UI,
    INTERACTION,
    MVI,
    SDUI,
    EXECUTION,
    WORKFLOW,
    NAVIGATION,
    COMMUNICATION,
    GRAPHQL,
    HTTP,
    WEBSOCKET,
    PERSISTENCE,
    CACHE,
    CAPABILITY,
    PAYMENT,
    LOCATION,
    BLE,
    PERFORMANCE,
    SECURITY,
    ERROR
}
