package com.carbroz.data.network

import kotlinx.coroutines.delay

/**
 * Per-request execution policy owned by the canonical network foundation.
 *
 * Retries are opt-in. The policy is intentionally deterministic; jitter can be introduced later
 * behind the same contract if production telemetry demonstrates a need for it.
 */
data class NetworkExecutionPolicy(
    val timeoutMillis: Long = DEFAULT_TIMEOUT_MILLIS,
    val maxAttempts: Int = 1,
    val initialRetryDelayMillis: Long = DEFAULT_RETRY_DELAY_MILLIS,
    val maxRetryDelayMillis: Long = DEFAULT_MAX_RETRY_DELAY_MILLIS,
    val backoffMultiplier: Double = DEFAULT_BACKOFF_MULTIPLIER,
) {
    init {
        require(timeoutMillis > 0) { "Network timeout must be > 0 ms" }
        require(maxAttempts in 1..MAX_ATTEMPTS) { "Network maxAttempts must be in 1..$MAX_ATTEMPTS" }
        require(initialRetryDelayMillis >= 0) { "Initial retry delay must be >= 0 ms" }
        require(maxRetryDelayMillis >= initialRetryDelayMillis) {
            "Maximum retry delay must be >= initial retry delay"
        }
        require(backoffMultiplier >= 1.0) { "Backoff multiplier must be >= 1.0" }
    }

    fun retryDelayMillis(retryIndex: Int): Long {
        require(retryIndex >= 0) { "Retry index must be >= 0" }
        var value = initialRetryDelayMillis.toDouble()
        repeat(retryIndex) {
            value = (value * backoffMultiplier).coerceAtMost(maxRetryDelayMillis.toDouble())
        }
        return value.toLong().coerceAtMost(maxRetryDelayMillis)
    }

    companion object {
        const val DEFAULT_TIMEOUT_MILLIS = 15_000L
        const val DEFAULT_RETRY_DELAY_MILLIS = 250L
        const val DEFAULT_MAX_RETRY_DELAY_MILLIS = 4_000L
        const val DEFAULT_BACKOFF_MULTIPLIER = 2.0
        const val MAX_ATTEMPTS = 5
    }
}

/** Injectable retry suspension boundary so retry policy tests never depend on wall-clock time. */
fun interface NetworkRetryDelay {
    suspend fun wait(delayMillis: Long)
}

object CoroutineNetworkRetryDelay : NetworkRetryDelay {
    override suspend fun wait(delayMillis: Long) {
        if (delayMillis > 0) delay(delayMillis)
    }
}

internal fun NetworkResult.isRetryable(): Boolean = when (this) {
    is NetworkResult.Success -> false
    is NetworkResult.Failure -> when (val failure = error) {
        NetworkFailure.Offline,
        NetworkFailure.Timeout,
        NetworkFailure.Transport,
        -> true

        is NetworkFailure.Http ->
            failure.statusCode == 408 || failure.statusCode == 429 || failure.statusCode in 500..599

        is NetworkFailure.InvalidRequest -> false
    }
}

internal fun NetworkMethod.requiresIdempotencyKeyForRetry(): Boolean =
    this == NetworkMethod.POST || this == NetworkMethod.PATCH
