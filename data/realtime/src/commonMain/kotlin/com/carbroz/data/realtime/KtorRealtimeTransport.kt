package com.carbroz.data.realtime

import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.websocket.WebSockets
import io.ktor.client.plugins.websocket.webSocketSession
import io.ktor.client.request.header
import io.ktor.client.request.url
import io.ktor.websocket.CloseReason
import io.ktor.websocket.Frame
import io.ktor.websocket.close
import io.ktor.websocket.readText
import io.ktor.websocket.send
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onCompletion

/** Ktor WebSocket adapter kept behind the neutral [RealtimeTransport] boundary. */
class KtorRealtimeTransport(
    private val client: HttpClient,
) : RealtimeTransport {
    override suspend fun connect(request: RealtimeConnectRequest): RealtimeConnection = try {
        val session = client.webSocketSession {
            url(request.url)
            request.headers.forEach { (name, value) -> header(name, value) }
        }
        KtorRealtimeConnection(session)
    } catch (error: CancellationException) {
        throw error
    } catch (_: Throwable) {
        FailedRealtimeConnection(RealtimeFailure.Transport)
    }
}

fun createKtorRealtimeTransport(): KtorRealtimeTransport =
    KtorRealtimeTransport(HttpClient(CIO) { install(WebSockets) })

private class KtorRealtimeConnection(
    private val session: io.ktor.client.plugins.websocket.DefaultClientWebSocketSession,
) : RealtimeConnection {
    private val mutableState = MutableStateFlow<RealtimeConnectionState>(RealtimeConnectionState.Connected)

    override val state: StateFlow<RealtimeConnectionState> = mutableState

    override val incoming: Flow<RealtimeMessage> = session.incoming
        .filterIsInstance<Frame.Text>()
        .map { frame -> RealtimeMessage(frame.readText()) }
        .onCompletion { cause ->
            mutableState.value = if (cause == null) {
                RealtimeConnectionState.Closed
            } else {
                RealtimeConnectionState.Failed(RealtimeFailure.Transport)
            }
        }

    override suspend fun send(message: RealtimeMessage) {
        session.send(Frame.Text(message.payload))
    }

    override suspend fun close() {
        if (mutableState.value == RealtimeConnectionState.Closed) return
        mutableState.value = RealtimeConnectionState.Closing
        session.close(CloseReason(CloseReason.Codes.NORMAL, "client_close"))
        mutableState.value = RealtimeConnectionState.Closed
    }
}

private class FailedRealtimeConnection(
    failure: RealtimeFailure,
) : RealtimeConnection {
    private val mutableState = MutableStateFlow<RealtimeConnectionState>(RealtimeConnectionState.Failed(failure))

    override val state: StateFlow<RealtimeConnectionState> = mutableState
    override val incoming: Flow<RealtimeMessage> = kotlinx.coroutines.flow.emptyFlow()

    override suspend fun send(message: RealtimeMessage) = Unit

    override suspend fun close() {
        mutableState.value = RealtimeConnectionState.Closed
    }
}
