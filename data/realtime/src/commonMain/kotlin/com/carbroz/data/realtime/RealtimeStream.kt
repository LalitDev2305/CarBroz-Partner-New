package com.carbroz.data.realtime

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flow

fun interface RealtimeRetryDelay {
    suspend fun wait(delayMillis: Long)
}

object CoroutineRealtimeRetryDelay : RealtimeRetryDelay {
    override suspend fun wait(delayMillis: Long) {
        if (delayMillis > 0L) delay(delayMillis)
    }
}

data class RealtimeReconnectPolicy(
    val maxReconnectAttempts: Int = 5,
    val initialDelayMillis: Long = 500L,
    val maxDelayMillis: Long = 8_000L,
) {
    init {
        require(maxReconnectAttempts >= 0) { "maxReconnectAttempts must be non-negative" }
        require(initialDelayMillis >= 0L) { "initialDelayMillis must be non-negative" }
        require(maxDelayMillis >= initialDelayMillis) { "maxDelayMillis must be >= initialDelayMillis" }
    }

    fun delayMillis(attemptIndex: Int): Long {
        require(attemptIndex >= 0) { "attemptIndex must be non-negative" }
        if (initialDelayMillis == 0L) return 0L
        val multiplier = 1L shl attemptIndex.coerceAtMost(30)
        return (initialDelayMillis * multiplier).coerceAtMost(maxDelayMillis)
    }
}

/**
 * Reconnects a realtime stream after transport closure/failure. Cancellation always propagates.
 * Subscription-specific resume cursors remain the responsibility of the operation/protocol adapter.
 */
class RealtimeStream(
    private val transport: RealtimeTransport,
    private val reconnectPolicy: RealtimeReconnectPolicy = RealtimeReconnectPolicy(),
    private val retryDelay: RealtimeRetryDelay = CoroutineRealtimeRetryDelay,
) {
    fun observe(request: RealtimeConnectRequest): Flow<RealtimeMessage> = flow {
        var reconnectAttempt = 0

        while (true) {
            val connection = transport.connect(request)
            val initialState = connection.state.value
            if (initialState is RealtimeConnectionState.Failed) {
                if (reconnectAttempt >= reconnectPolicy.maxReconnectAttempts) return@flow
                retryDelay.wait(reconnectPolicy.delayMillis(reconnectAttempt++))
                continue
            }

            try {
                connection.incoming.collect { emit(it) }
            } catch (error: CancellationException) {
                connection.close()
                throw error
            } finally {
                connection.close()
            }

            if (reconnectAttempt >= reconnectPolicy.maxReconnectAttempts) return@flow
            retryDelay.wait(reconnectPolicy.delayMillis(reconnectAttempt++))
        }
    }
}
