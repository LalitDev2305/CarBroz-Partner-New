package com.carbroz.data.network

/** Stable identity for one logical network execution, including retries and auth recovery. */
@JvmInline
value class NetworkRequestId(val value: String) {
    init {
        require(value.isNotBlank()) { "Network request id must not be blank" }
        require(value.length <= 128) { "Network request id must be <= 128 characters" }
    }
}

/** Supplies request identities without forcing a platform-specific UUID dependency into common code. */
fun interface NetworkRequestIdProvider {
    fun nextId(): NetworkRequestId
}

/** Default provider for compositions that do not need request correlation yet. */
data object EmptyNetworkRequestIdProvider : NetworkRequestIdProvider {
    override fun nextId(): NetworkRequestId = NetworkRequestId("untracked")
}

data class NetworkRequestContext(
    val requestId: NetworkRequestId,
    val method: NetworkMethod,
    val endpoint: NetworkEndpoint,
)

sealed interface NetworkObservation {
    val context: NetworkRequestContext

    data class Started(override val context: NetworkRequestContext) : NetworkObservation

    data class AttemptStarted(
        override val context: NetworkRequestContext,
        val attempt: Int,
    ) : NetworkObservation

    data class RetryScheduled(
        override val context: NetworkRequestContext,
        val attempt: Int,
        val delayMillis: Long,
    ) : NetworkObservation

    data class AuthenticationRecoveryStarted(
        override val context: NetworkRequestContext,
    ) : NetworkObservation

    data class AuthenticationRecoveryFinished(
        override val context: NetworkRequestContext,
        val recovered: Boolean,
    ) : NetworkObservation

    data class Finished(
        override val context: NetworkRequestContext,
        val outcome: NetworkOutcome,
    ) : NetworkObservation
}

sealed interface NetworkOutcome {
    data class Success(val statusCode: Int) : NetworkOutcome
    data class HttpFailure(val statusCode: Int) : NetworkOutcome
    data object Offline : NetworkOutcome
    data object Timeout : NetworkOutcome
    data object TransportFailure : NetworkOutcome
    data object InvalidRequest : NetworkOutcome
}

fun interface NetworkObserver {
    fun observe(event: NetworkObservation)
}

data object NoNetworkObserver : NetworkObserver {
    override fun observe(event: NetworkObservation) = Unit
}

internal fun NetworkResult.toNetworkOutcome(): NetworkOutcome = when (this) {
    is NetworkResult.Success -> NetworkOutcome.Success(response.statusCode)
    is NetworkResult.Failure -> when (val failure = error) {
        NetworkFailure.Offline -> NetworkOutcome.Offline
        NetworkFailure.Timeout -> NetworkOutcome.Timeout
        is NetworkFailure.Http -> NetworkOutcome.HttpFailure(failure.statusCode)
        is NetworkFailure.Transport -> NetworkOutcome.TransportFailure
        is NetworkFailure.InvalidRequest -> NetworkOutcome.InvalidRequest
    }
}
