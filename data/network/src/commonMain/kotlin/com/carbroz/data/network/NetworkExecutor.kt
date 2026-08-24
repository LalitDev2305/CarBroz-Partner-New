package com.carbroz.data.network

import com.carbroz.foundation.time.Clock
import com.carbroz.foundation.time.SystemClock
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
    private val authorizationProvider: NetworkAuthorizationProvider = EmptyNetworkAuthorizationProvider,
    private val authenticationRecovery: NetworkAuthenticationRecovery = NoNetworkAuthenticationRecovery,
    private val retryDelay: NetworkRetryDelay = CoroutineNetworkRetryDelay,
    private val connectivityProvider: NetworkConnectivityProvider = UnknownNetworkConnectivityProvider,
    private val requestIdProvider: NetworkRequestIdProvider = EmptyNetworkRequestIdProvider,
    private val observer: NetworkObserver = NoNetworkObserver,
    private val responseCache: NetworkResponseCache = NoNetworkResponseCache,
    private val clock: Clock = SystemClock,
    private val json: Json = Json,
) {
    suspend fun execute(request: NetworkRequest): NetworkResult {
        val context = NetworkRequestContext(
            requestId = requestIdProvider.nextId(),
            method = request.method,
            endpoint = request.endpoint,
        )
        observer.observe(NetworkObservation.Started(context))

        val validationFailure = validateExecution(request)
        if (validationFailure != null) return finish(context, validationFailure)

        val firstResult = executeWithCachePolicy(request, context)
        if (!request.shouldAttemptAuthenticationRecovery(firstResult)) return finish(context, firstResult)

        observer.observe(NetworkObservation.AuthenticationRecoveryStarted(context))
        val recovered = authenticationRecovery.recover()
        observer.observe(NetworkObservation.AuthenticationRecoveryFinished(context, recovered))
        if (!recovered) return finish(context, firstResult)

        return finish(context, executeWithCachePolicy(request, context))
    }

    private suspend fun executeWithCachePolicy(
        request: NetworkRequest,
        context: NetworkRequestContext,
    ): NetworkResult {
        val key = request.cacheKey()
        return when (val policy = request.cachePolicy) {
            NetworkCachePolicy.NetworkOnly -> executeWithPolicy(request, context)
            is NetworkCachePolicy.CacheFirst -> {
                responseCache.freshResult(key, policy.maxAgeMillis) ?: executeAndCache(request, context, key)
            }
            is NetworkCachePolicy.NetworkFirst -> {
                val networkResult = executeAndCache(request, context, key)
                if (networkResult is NetworkResult.Success) networkResult
                else responseCache.freshResult(key, policy.fallbackMaxAgeMillis) ?: networkResult
            }
        }
    }

    private suspend fun executeAndCache(
        request: NetworkRequest,
        context: NetworkRequestContext,
        key: NetworkCacheKey,
    ): NetworkResult {
        val result = executeWithPolicy(request, context)
        val success = result as? NetworkResult.Success ?: return result
        if (!success.response.disallowsStorage()) {
            responseCache.put(
                key = key,
                entry = NetworkCacheEntry(
                    response = success.response,
                    storedAtEpochMillis = clock.nowEpochMilliseconds(),
                ),
            )
        }
        return result
    }

    private suspend fun NetworkResponseCache.freshResult(
        key: NetworkCacheKey,
        maxAgeMillis: Long,
    ): NetworkResult.Success? {
        val entry = get(key) ?: return null
        val age = (clock.nowEpochMilliseconds() - entry.storedAtEpochMillis).coerceAtLeast(0L)
        return if (age <= maxAgeMillis) NetworkResult.Success(entry.response) else null
    }

    private suspend fun executeWithPolicy(
        request: NetworkRequest,
        context: NetworkRequestContext,
    ): NetworkResult {
        val transportRequest = try {
            buildTransportRequest(request)
        } catch (error: IllegalArgumentException) {
            return NetworkResult.Failure(NetworkFailure.InvalidRequest(error.message.orEmpty()))
        } ?: return NetworkResult.Failure(NetworkFailure.InvalidRequest("Authenticated session is required"))

        repeat(request.executionPolicy.maxAttempts) { attemptIndex ->
            if (connectivityProvider.connectivity() == NetworkConnectivity.OFFLINE) {
                return NetworkResult.Failure(NetworkFailure.Offline)
            }

            val attempt = attemptIndex + 1
            observer.observe(NetworkObservation.AttemptStarted(context, attempt))
            val result = withTimeoutOrNull(request.executionPolicy.timeoutMillis) {
                transport.execute(transportRequest)
            } ?: NetworkResult.Failure(NetworkFailure.Timeout)

            val hasAnotherAttempt = attempt < request.executionPolicy.maxAttempts
            if (!hasAnotherAttempt || !result.isRetryable()) return result

            val delayMillis = request.executionPolicy.retryDelayMillis(attemptIndex)
            observer.observe(NetworkObservation.RetryScheduled(context, attempt, delayMillis))
            retryDelay.wait(delayMillis)
        }

        error("Network retry loop exhausted without returning a result")
    }

    private suspend fun buildTransportRequest(request: NetworkRequest): TransportRequest? {
        val headers = buildMap<String, String> {
            putAll(headerPolicy.merge(headerProvider.headers(), request.headers))
            request.idempotencyKey?.let { key -> put(IDEMPOTENCY_HEADER, key) }
            if (request.authentication == NetworkAuthentication.SESSION) {
                val authorization = authorizationProvider.authorizationHeader() ?: return null
                put(AUTHORIZATION_HEADER, authorization)
            }
        }

        return TransportRequest(
            method = request.method.name,
            url = environment.resolve(request.endpoint),
            headers = headers,
            body = request.payload?.let { payload -> json.encodeToString(payload) },
        )
    }

    private fun validateExecution(request: NetworkRequest): NetworkResult.Failure? {
        val retriesEnabled = request.executionPolicy.maxAttempts > 1
        if (retriesEnabled && request.method.requiresIdempotencyKeyForRetry() && request.idempotencyKey == null) {
            return NetworkResult.Failure(
                NetworkFailure.InvalidRequest("${request.method} retries require an idempotency key"),
            )
        }
        if (request.cachePolicy != NetworkCachePolicy.NetworkOnly) {
            if (request.method != NetworkMethod.GET) {
                return NetworkResult.Failure(NetworkFailure.InvalidRequest("Only GET requests may use response caching"))
            }
            if (request.authentication != NetworkAuthentication.NONE) {
                return NetworkResult.Failure(
                    NetworkFailure.InvalidRequest("Authenticated responses require repository-owned scoped caching"),
                )
            }
        }
        return null
    }

    private fun NetworkRequest.cacheKey(): NetworkCacheKey = NetworkCacheKey(
        endpoint = endpoint,
        headers = headers,
    )

    private fun NetworkResponse.disallowsStorage(): Boolean = headers.entries.any { (name, value) ->
        name.equals(CACHE_CONTROL_HEADER, ignoreCase = true) &&
            value.split(',').any { directive -> directive.trim().equals("no-store", ignoreCase = true) }
    }

    private fun NetworkRequest.shouldAttemptAuthenticationRecovery(result: NetworkResult): Boolean {
        if (authentication != NetworkAuthentication.SESSION) return false
        val http = (result as? NetworkResult.Failure)?.error as? NetworkFailure.Http ?: return false
        if (http.statusCode != 401) return false
        return !method.requiresIdempotencyKeyForRetry() || idempotencyKey != null
    }

    private fun finish(context: NetworkRequestContext, result: NetworkResult): NetworkResult {
        observer.observe(NetworkObservation.Finished(context, result.toNetworkOutcome()))
        return result
    }

    private companion object {
        const val AUTHORIZATION_HEADER = "Authorization"
        const val IDEMPOTENCY_HEADER = "Idempotency-Key"
        const val CACHE_CONTROL_HEADER = "Cache-Control"
    }
}
