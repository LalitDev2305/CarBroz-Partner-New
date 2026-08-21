package com.carbroz.foundation.configuration

/**
 * Identifies the runtime environment selected for an application build.
 *
 * Product code should depend on this semantic value rather than inspect build
 * constants or environment strings directly.
 */
enum class AppEnvironment {
    Development,
    Staging,
    Production,
}
