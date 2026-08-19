package com.carbroz.partner.app.composition.config

/**
 * Immutable application environment configuration parameters.
 *
 * Owned by `:app:composition` to supply environment-level properties (such as backend [baseUrl])
 * to the application composition graph.
 *
 * @param baseUrl Base URL for backend network API transport calls (e.g. `"https://api.carbroz.com"`).
 */
public data class AppConfig(
    val baseUrl: String
)
