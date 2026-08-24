package com.carbroz.data.network

import com.carbroz.foundation.configuration.AppConfiguration
import com.carbroz.foundation.configuration.AppEnvironment
import com.carbroz.foundation.configuration.ConfigurationProvider
import com.carbroz.foundation.configuration.ConfigurationValidationResult
import com.carbroz.foundation.configuration.ConfigurationValidator

/** Trusted app-owned origin. Server-driven commands may provide only relative endpoints. */
data class NetworkEnvironment(
    val baseUrl: String,
    val appEnvironment: AppEnvironment = AppEnvironment.Production,
) {
    init {
        val https = baseUrl.startsWith("https://")
        val localDevelopmentHttp = appEnvironment == AppEnvironment.Development &&
            (baseUrl.startsWith("http://localhost") || baseUrl.startsWith("http://127.0.0.1"))
        require(https || localDevelopmentHttp) {
            "Network baseUrl must use HTTPS except for local Development endpoints"
        }
        require(!baseUrl.endsWith('/')) { "Network baseUrl must not end with /" }
        require('#' !in baseUrl) { "Network baseUrl must not contain a fragment" }
        require('?' !in baseUrl) { "Network baseUrl must not contain a query" }
    }

    fun resolve(endpoint: NetworkEndpoint): String = baseUrl + endpoint.value
}

/**
 * Adapts the canonical application configuration into the network-owned origin model.
 * Configuration remains owned by foundation:configuration; networking does not duplicate
 * environment selection or endpoint values.
 */
class NetworkEnvironmentProvider(
    private val configurationProvider: ConfigurationProvider,
) {
    fun get(): NetworkEnvironment = configurationProvider.get().toNetworkEnvironment()
}

/** Creates a network environment only from configuration accepted by the canonical validator. */
fun AppConfiguration.toNetworkEnvironment(): NetworkEnvironment {
    val validation = ConfigurationValidator.validate(this)
    require(validation is ConfigurationValidationResult.Valid) {
        "Cannot initialize networking from invalid application configuration"
    }
    return NetworkEnvironment(
        baseUrl = apiBaseUrl,
        appEnvironment = environment,
    )
}

fun interface NetworkHeaderProvider {
    suspend fun headers(): Map<String, String>
}

object EmptyNetworkHeaderProvider : NetworkHeaderProvider {
    override suspend fun headers(): Map<String, String> = emptyMap()
}

/** Prevents dynamic commands from overriding or injecting transport-owned HTTP headers. */
class NetworkHeaderPolicy(
    private val reservedNames: Set<String> = setOf(
        "authorization",
        "host",
        "content-length",
        "idempotency-key",
    ),
) {
    fun merge(
        transportHeaders: Map<String, String>,
        requestHeaders: Map<String, String>,
    ): Map<String, String> {
        validateHeaders(transportHeaders)
        validateHeaders(requestHeaders)

        val forbidden = requestHeaders.keys.firstOrNull { it.lowercase() in reservedNames }
        require(forbidden == null) { "Request cannot override reserved header: $forbidden" }
        return transportHeaders + requestHeaders
    }

    private fun validateHeaders(headers: Map<String, String>) {
        headers.forEach { (name, value) ->
            require(name.isNotBlank()) { "Header name must not be blank" }
            require(name.all(::isValidHeaderNameCharacter)) { "Header name contains invalid characters" }
            require(value.none(::isForbiddenHeaderValueCharacter)) { "Header value contains invalid control characters" }
        }
    }

    private fun isValidHeaderNameCharacter(character: Char): Boolean =
        character in 'a'..'z' ||
            character in 'A'..'Z' ||
            character in '0'..'9' ||
            character in "!#$%&'*+-.^_`|~"

    private fun isForbiddenHeaderValueCharacter(character: Char): Boolean =
        character == '\r' || character == '\n' || character.code == 0x7F || character.code in 0x00..0x08 ||
            character.code in 0x0B..0x0C || character.code in 0x0E..0x1F
}
