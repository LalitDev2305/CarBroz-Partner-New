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
    private val authorizationProvider: NetworkAuthorizationProvider = EmptyNetworkAuthorizationProvider,
    private val authenticationRecovery: NetworkAuthenticationRecovery = NoNetworkAuthenticationRecovery,
    private val retryDelay: NetworkRetryDelay = CoroutineNetworkRetryDelay,
    private val requestIdProvider: NetworkRequestIdProvider = EmptyNetworkRequestIdProvider,
    private val observer: NetworkObserver = NoNetworkObserver,
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

        val firstResult = executeWithPolicy(request, context)
        if (!request.shouldAttemptAuthenticationRecovery(firstResult)) return finish(context, firstResult)

        observer.observe(NetworkObservation.AuthenticationRecoveryStarted(context))
        val recovered = authenticationRecovery.recover()
        observer.observe(NetworkObservation.AuthenticationRecoveryFinished(context, recovered))
        if (!recovered) return finish(context, firstResult)

        return finish(context, executeWithPolicy(request, context))
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
        return null
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
    }
}
