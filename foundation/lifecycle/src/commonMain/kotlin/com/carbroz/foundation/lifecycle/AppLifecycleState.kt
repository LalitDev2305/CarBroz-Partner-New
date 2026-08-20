package com.carbroz.foundation.lifecycle

/**
 * Product-neutral visibility state for the running application.
 *
 * `Unknown` is used until the platform host reports a reliable state. A terminal
 * state is intentionally absent because process termination is not consistently
 * observable across Android, iOS, and Desktop.
 */
enum class AppLifecycleState {
    Unknown,
    Foreground,
    Background,
}
