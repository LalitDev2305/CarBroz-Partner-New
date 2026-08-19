package com.carbroz.partner.app.composition.config

/**
 * Technical SDUI entry endpoint configuration owned by `:app:composition`.
 *
 * Provides the single canonical fallback technical path requested by the Screen Runtime Coordinator
 * when acquiring the root server-driven UI document post-Splash bootstrap.
 *
 * @param entryEndpoint Path relative to base URL (e.g. `"/api/v1/sdui/root"`).
 */
public data class SduiEndpointConfig(
    val entryEndpoint: String = ROOT_SDUI_ENDPOINT
) {
    public companion object {
        public const val ROOT_SDUI_ENDPOINT: String = "/api/v1/sdui/root"
    }
}
