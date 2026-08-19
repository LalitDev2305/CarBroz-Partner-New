package com.carbroz.partner.app.composition.config

/**
 * Immutable application environment configuration parameters.
 *
 * Encapsulates environment configuration properties supplied to [AppGraph].
 *
 * @param baseUrl Base URL for backend network API transport calls.
 */
public data class AppConfig(
    val baseUrl: String
) {
    public companion object {
        /**
         * Factory producing default production environment configuration.
         */
        public fun production(): AppConfig = AppConfig(
            baseUrl = "https://api.carbroz.com"
        )
    }
}
