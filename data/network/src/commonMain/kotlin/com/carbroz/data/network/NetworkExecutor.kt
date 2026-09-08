package com.carbroz.data.network

import com.carbroz.foundation.observability.CorrelationId
import com.carbroz.foundation.observability.DiagnosticAttribute
import com.carbroz.foundation.observability.LogEvent
import com.carbroz.foundation.observability.LogLevel
import com.carbroz.foundation.observability.NoOpObservability
import com.carbroz.foundation.observability.Observability
import com.carbroz.foundation.observability.PerformanceMetric
import com.carbroz.foundation.observability.TraceOutcome
import com.carbroz.foundation.observability.TraceSpan
import com.carbroz.foundation.time.Clock
import com.carbroz.foundation.time.SystemClock
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.serialization.json.Json

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
    private val observability: Observability = NoOpObservability,
    private val json: Json = Json,
) {
    suspend fun execute(request: NetworkRequest): NetworkResult {
        val startedAt = clock.nowEpochMilliseconds()
        val context = NetworkRequestContext(
            requestId = requestIdProvider.nextId(),
            method = request.method,
            endpoint = request.endpoint,
        )
        observer.observe(NetworkObservation.Started(context))

        return try {
            val validationFailure = validateExecution(request)
            if (validationFailure != null) return finish(context, validationFailure, startedAt)

            val firstResult = executeWithCachePolicy(request, context)
            if (!request.shouldAttemptAuthenticationRecovery(firstResult)) return finish(context, firstResult, startedAt)

            observer.observe(NetworkObservation.AuthenticationRecoveryStarted(context))
            val recovery = authenticationRecovery.recover()
            observer.observe(
                NetworkObservation.AuthenticationRecoveryFinished(
                    context = context,
                    recovered = recovery != NetworkAuthenticationRecoveryResult.Unavailable,
                ),
            )

            val shouldRetry = when (recovery) {
                NetworkAuthenticationRecoveryResult.Recovered -> true
                NetworkAuthenticationRecoveryResult.SessionInvalidated -> false
                NetworkAuthenticationRecoveryResult.Unavailable -> false
            }
            if (!shouldRetry) return finish(context, firstResult, startedAt)

            finish(context, executeWithCachePolicy(request, context), startedAt)
        } catch (cancellation: CancellationException) {
            observability.trace(
                TraceSpan(
                    name = "network.request",
                    correlationId = context.correlationId(),
                    durationMillis = elapsedSince(startedAt),
                    outcome = TraceOutcome.CANCELLED,
                    attributes = mapOf("method" to DiagnosticAttribute(context.method.name)),
                ),
            )
            throw cancellation
        }
    }

    private suspend fun executeWithCachePolicy(request: NetworkRequest, context: NetworkRequestContext): NetworkResult {
        val key = request.cacheKey()
        return when (val policy = request.cachePolicy) {
            NetworkCachePolicy.NetworkOnly -> executeWithPolicy(request, context)
            is NetworkCachePolicy.CacheFirst ->
                responseCache.freshResult(key, policy.maxAgeMillis) ?: executeAndCache(request, context, key)
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
                key,
                NetworkCacheEntry(success.response, clock.nowEpochMilliseconds()),
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

    private suspend fun executeWithPolicy(request: NetworkRequest, context: NetworkRequestContext): NetworkResult {
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
            when (request.authentication) {
                NetworkAuthentication.NONE -> Unit
                NetworkAuthentication.SESSION -> {
                    val authorization = authorizationProvider.authorizationHeader() ?: return null
                    put(AUTHORIZATION_HEADER, authorization)
                }
                NetworkAuthentication.OPTIONAL_SESSION -> {
                    authorizationProvider.authorizationHeader()?.let { put(AUTHORIZATION_HEADER, it) }
                }
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

    private fun NetworkRequest.cacheKey(): NetworkCacheKey = NetworkCacheKey(endpoint = endpoint, headers = headers)

    private fun NetworkResponse.disallowsStorage(): Boolean = headers.entries.any { (name, value) ->
        name.equals(CACHE_CONTROL_HEADER, ignoreCase = true) &&
            value.split(',').any { directive -> directive.trim().equals("no-store", ignoreCase = true) }
    }

    private fun NetworkRequest.shouldAttemptAuthenticationRecovery(result: NetworkResult): Boolean {
        if (authentication == NetworkAuthentication.NONE) return false
        val http = (result as? NetworkResult.Failure)?.error as? NetworkFailure.Http ?: return false
        if (http.statusCode != 401) return false
        return !method.requiresIdempotencyKeyForRetry() || idempotencyKey != null
    }

    private fun finish(
        context: NetworkRequestContext,
        result: NetworkResult,
        startedAt: Long,
    ): NetworkResult {
        val outcome = result.toNetworkOutcome()
        val duration = elapsedSince(startedAt)
        val correlationId = context.correlationId()
        val attributes = mapOf(
            "method" to DiagnosticAttribute(context.method.name),
            "outcome" to DiagnosticAttribute(outcome.metricName()),
        )
        observer.observe(NetworkObservation.Finished(context, outcome))
        observability.performance(
            PerformanceMetric(
                name = "network.request",
                durationMillis = duration,
                correlationId = correlationId,
                attributes = attributes,
            ),
        )
        observability.trace(
            TraceSpan(
                name = "network.request",
                correlationId = correlationId,
                durationMillis = duration,
                outcome = if (outcome is NetworkOutcome.Success) TraceOutcome.SUCCESS else TraceOutcome.FAILURE,
                attributes = attributes,
            ),
        )
        if (outcome !is NetworkOutcome.Success) {
            observability.log(
                LogEvent(
                    level = LogLevel.WARN,
                    category = "network",
                    message = "network_request_failed",
                    correlationId = correlationId,
                    attributes = attributes,
                ),
            )
        }
        return result
    }

    private fun NetworkRequestContext.correlationId(): CorrelationId = CorrelationId(requestId.value)
    private fun elapsedSince(startedAt: Long): Long = (clock.nowEpochMilliseconds() - startedAt).coerceAtLeast(0L)

    private fun NetworkOutcome.metricName(): String = when (this) {
        is NetworkOutcome.Success -> "success"
        is NetworkOutcome.HttpFailure -> "http_failure"
        NetworkOutcome.Offline -> "offline"
        NetworkOutcome.Timeout -> "timeout"
        NetworkOutcome.TransportFailure -> "transport_failure"
        NetworkOutcome.InvalidRequest -> "invalid_request"
    }

    private companion object {
        const val AUTHORIZATION_HEADER = "Authorization"
        const val IDEMPOTENCY_HEADER = "Idempotency-Key"
        const val CACHE_CONTROL_HEADER = "Cache-Control"
    }
}
