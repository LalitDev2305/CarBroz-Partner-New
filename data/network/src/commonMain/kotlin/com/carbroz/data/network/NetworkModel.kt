package com.carbroz.data.network

import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject

@kotlin.jvm.JvmInline
value class NetworkEndpoint(val value: String) {
    init {
        require(value.startsWith('/')) { "Network endpoint must be relative and start with /" }
        require(!value.startsWith("//")) { "Network endpoint cannot be protocol-relative" }
        require("://" !in value) { "Network endpoint cannot contain an absolute URL" }
        require(value.length <= 2048) { "Network endpoint is too long" }
    }
}

enum class NetworkMethod { GET, POST, PUT, PATCH, DELETE }

enum class NetworkAuthentication { NONE, SESSION }

data class NetworkRequest(
    val method: NetworkMethod,
    val endpoint: NetworkEndpoint,
    val payload: JsonObject? = null,
    val headers: Map<String, String> = emptyMap(),
    val executionPolicy: NetworkExecutionPolicy = NetworkExecutionPolicy(),
    val idempotencyKey: String? = null,
    val authentication: NetworkAuthentication = NetworkAuthentication.NONE,
) {
    init {
        idempotencyKey?.let { key ->
            require(key.isNotBlank()) { "Idempotency key must not be blank" }
            require(key.length <= 128) { "Idempotency key must be <= 128 characters" }
        }
    }
}

data class NetworkResponse(
    val statusCode: Int,
    val headers: Map<String, String> = emptyMap(),
    val body: JsonElement? = null,
)

sealed interface NetworkFailure {
    data object Offline : NetworkFailure
    data object Timeout : NetworkFailure
    data class Http(val statusCode: Int, val body: JsonElement? = null) : NetworkFailure
    data class Transport(val reason: String? = null) : NetworkFailure
    data class InvalidRequest(val reason: String) : NetworkFailure
}

sealed interface NetworkResult {
    data class Success(val response: NetworkResponse) : NetworkResult
    data class Failure(val error: NetworkFailure) : NetworkResult
}
