package com.carbroz.data.network

import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.serialization.json.Json

/** Transport SPI. Ktor belongs behind this interface, not in runtime:action. */
fun interface NetworkTransport {
    suspend fun execute(request: TransportRequest): NetworkResult
}

data class TransportRequest(
    val method: String,
    val url: String,
    val headers: Map<String, String>,
    val body: String?,
)

class NetworkExecutor(
    private val environment: NetworkEnvironment,
    private val transport: NetworkTransport,
    private val headerProvider: NetworkHeaderProvider = EmptyNetworkHeaderProvider,
    private val headerPolicy: NetworkHeaderPolicy = NetworkHeaderPolicy(),
    private val retryDelay: NetworkRetryDelay = CoroutineNetworkRetryDelay,
    private val json: Json = Json,
) {
    suspend fun execute(request: NetworkRequest): NetworkResult {
        val validationFailure = validateExecution(request)
        if (validationFailure != null) return validationFailure

        val headers = try {
            buildMap<String, String> {
                putAll(headerPolicy.merge(headerProvider.headers(), request.headers))
                request.idempotencyKey?.let { key -> put(IDEMPOTENCY_HEADER, key) }
            }
        } catch (error: IllegalArgumentException) {
            return NetworkResult.Failure(NetworkFailure.InvalidRequest(error.message.orEmpty()))
        }

        val transportRequest = TransportRequest(
            method = request.method.name,
            url = environment.resolve(request.endpoint),
            headers = headers,
            body = request.payload?.let { payload -> json.encodeToString(payload) },
        )

        repeat(request.executionPolicy.maxAttempts) { attemptIndex ->
            val result = withTimeoutOrNull(request.executionPolicy.timeoutMillis) {
                transport.execute(transportRequest)
            } ?: NetworkResult.Failure(NetworkFailure.Timeout)

            val hasAnotherAttempt = attemptIndex + 1 < request.executionPolicy.maxAttempts
            if (!hasAnotherAttempt || !result.isRetryable()) return result

            retryDelay.wait(request.executionPolicy.retryDelayMillis(attemptIndex))
        }

        error("Network retry loop exhausted without returning a result")
    }

    private fun validateExecution(request: NetworkRequest): NetworkResult.Failure? {
        val retriesEnabled = request.executionPolicy.maxAttempts > 1
        if (retriesEnabled && request.method.requiresIdempotencyKeyForRetry() && request.idempotencyKey == null) {
            return NetworkResult.Failure(
                NetworkFailure.InvalidRequest(
                    "${request.method} retries require an idempotency key",
                ),
            )
        }
        return null
    }

    private companion object {
        const val IDEMPOTENCY_HEADER = "Idempotency-Key"
    }
}
