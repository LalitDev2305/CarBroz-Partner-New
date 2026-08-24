package com.carbroz.data.realtime

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

/** Neutral realtime transport contract consumed by higher layers. */
fun interface RealtimeTransport {
    suspend fun connect(request: RealtimeConnectRequest): RealtimeConnection
}

data class RealtimeConnectRequest(
    val url: String,
    val headers: Map<String, String> = emptyMap(),
)

interface RealtimeConnection {
    val state: StateFlow<RealtimeConnectionState>
    val incoming: Flow<RealtimeMessage>

    suspend fun send(message: RealtimeMessage)
    suspend fun close()
}

sealed interface RealtimeConnectionState {
    data object Connecting : RealtimeConnectionState
    data object Connected : RealtimeConnectionState
    data object Closing : RealtimeConnectionState
    data object Closed : RealtimeConnectionState
    data class Failed(val failure: RealtimeFailure) : RealtimeConnectionState
}

data class RealtimeMessage(
    val payload: String,
)

sealed interface RealtimeFailure {
    data object Transport : RealtimeFailure
    data object Protocol : RealtimeFailure
    data object Unauthorized : RealtimeFailure
}
