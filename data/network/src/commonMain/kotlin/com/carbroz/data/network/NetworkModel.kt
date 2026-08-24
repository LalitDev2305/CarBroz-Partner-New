package com.carbroz.data.network

import com.carbroz.runtime.action.PreparedAction
import com.carbroz.runtime.sdui.model.RequestMethod
import com.carbroz.runtime.sdui.model.ScreenDestination
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject

@JvmInline
value class NetworkEndpoint(val value: String) {
    init {
        require(value.startsWith('/')) { "Network endpoint must be relative and start with /" }
        require(!value.startsWith("//")) { "Network endpoint cannot be protocol-relative" }
        require("://" !in value) { "Network endpoint cannot contain an absolute URL" }
        require(value.length <= 2048) { "Network endpoint is too long" }
    }
}

data class NetworkRequest(
    val method: RequestMethod,
    val endpoint: NetworkEndpoint,
    val payload: JsonObject,
    val destination: ScreenDestination,
    val headers: Map<String, String> = emptyMap(),
)

data class NetworkResponse(
    val statusCode: Int,
    val headers: Map<String, String> = emptyMap(),
    val body: JsonElement? = null,
)

sealed interface NetworkFailure {
    data object Offline : NetworkFailure
    data object Timeout : NetworkFailure
    data object Cancelled : NetworkFailure
    data class Http(val statusCode: Int, val body: JsonElement? = null) : NetworkFailure
    data class Transport(val reason: String? = null) : NetworkFailure
    data class InvalidRequest(val reason: String) : NetworkFailure
}

sealed interface NetworkResult {
    data class Success(val response: NetworkResponse) : NetworkResult
    data class Failure(val error: NetworkFailure) : NetworkResult
}

fun PreparedAction.Request.toNetworkRequest(
    headers: Map<String, String> = emptyMap(),
): NetworkRequest = NetworkRequest(
    method = method,
    endpoint = NetworkEndpoint(endpoint),
    payload = payload,
    destination = destination,
    headers = headers,
)
