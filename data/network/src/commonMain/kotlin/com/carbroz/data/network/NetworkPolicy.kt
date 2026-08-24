package com.carbroz.data.network

/** Trusted app-owned origin. Server-driven commands may provide only relative endpoints. */
data class NetworkEnvironment(
    val baseUrl: String,
) {
    init {
        require(baseUrl.startsWith("https://")) { "Network baseUrl must use HTTPS" }
        require(!baseUrl.endsWith('/')) { "Network baseUrl must not end with /" }
    }

    fun resolve(endpoint: NetworkEndpoint): String = baseUrl + endpoint.value
}

fun interface NetworkHeaderProvider {
    suspend fun headers(): Map<String, String>
}

object EmptyNetworkHeaderProvider : NetworkHeaderProvider {
    override suspend fun headers(): Map<String, String> = emptyMap()
}

/** Prevents dynamic commands from overriding transport-owned security headers. */
class NetworkHeaderPolicy(
    private val reservedNames: Set<String> = setOf("authorization", "host", "content-length"),
) {
    fun merge(
        transportHeaders: Map<String, String>,
        requestHeaders: Map<String, String>,
    ): Map<String, String> {
        val forbidden = requestHeaders.keys.firstOrNull { it.lowercase() in reservedNames }
        require(forbidden == null) { "Request cannot override reserved header: $forbidden" }
        return transportHeaders + requestHeaders
    }
}
