package com.carbroz.data.network

import kotlinx.coroutines.CancellationException
import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromJsonElement

/** Result of decoding a successful network response into a caller-owned DTO type. */
sealed interface NetworkDecodedResult<out T> {
    data class Success<T>(
        val value: T,
        val response: NetworkResponse,
    ) : NetworkDecodedResult<T>

    data class Failure(
        val reason: NetworkDecodeFailure,
    ) : NetworkDecodedResult<Nothing>
}

/** Stable failures produced by the canonical typed response-decoding boundary. */
sealed interface NetworkDecodeFailure {
    data class Network(val failure: NetworkFailure) : NetworkDecodeFailure
    data object MissingBody : NetworkDecodeFailure
    data object InvalidBody : NetworkDecodeFailure
}

private val networkResponseJson = Json {
    ignoreUnknownKeys = true
}

/**
 * Executes through the canonical [NetworkDataSource] and decodes a successful JSON body with the
 * caller-owned serializer. Transport/auth/retry/cache behavior remains entirely in [NetworkExecutor].
 *
 * The raw [NetworkDataSource.execute] path remains available for protocol runtimes that intentionally
 * consume generic JSON rather than an API-specific DTO.
 */
suspend fun <T> NetworkDataSource.executeTyped(
    request: NetworkRequest,
    deserializer: DeserializationStrategy<T>,
): NetworkDecodedResult<T> = when (val result = execute(request)) {
    is NetworkResult.Failure -> NetworkDecodedResult.Failure(NetworkDecodeFailure.Network(result.error))
    is NetworkResult.Success -> {
        val body = result.response.body
            ?: return NetworkDecodedResult.Failure(NetworkDecodeFailure.MissingBody)

        val decoded = try {
            networkResponseJson.decodeFromJsonElement(deserializer, body)
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (_: Throwable) {
            return NetworkDecodedResult.Failure(NetworkDecodeFailure.InvalidBody)
        }

        NetworkDecodedResult.Success(
            value = decoded,
            response = result.response,
        )
    }
}
