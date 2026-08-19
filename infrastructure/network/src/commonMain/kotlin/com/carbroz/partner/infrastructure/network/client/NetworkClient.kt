package com.carbroz.partner.infrastructure.network.client

public data class NetworkRequest(
    val url: String,
    val method: String = "GET",
    val headers: Map<String, String> = emptyMap(),
    val bodyJson: String? = null
)

public data class NetworkResponse(
    val statusCode: Int,
    val bodyJson: String,
    val isSuccessful: Boolean = statusCode in 200..299
)

public interface NetworkClient {
    public suspend fun execute(request: NetworkRequest): NetworkResponse
}
