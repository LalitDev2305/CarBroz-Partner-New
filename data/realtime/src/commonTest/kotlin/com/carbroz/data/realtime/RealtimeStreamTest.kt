package com.carbroz.data.realtime

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class RealtimeStreamTest {
    @Test
    fun reconnectsAfterClosedConnectionAndEmitsSubsequentMessages() = runTest {
        val attempts = ArrayDeque<RealtimeConnection>()
        attempts += FakeConnection(messages = listOf(RealtimeMessage("first")))
        attempts += FakeConnection(messages = listOf(RealtimeMessage("second")))
        attempts += FailedConnection()
        val delays = mutableListOf<Long>()
        val stream = RealtimeStream(
            transport = RealtimeTransport { attempts.removeFirst() },
            reconnectPolicy = RealtimeReconnectPolicy(maxReconnectAttempts = 2, initialDelayMillis = 10, maxDelayMillis = 20),
            retryDelay = RealtimeRetryDelay { delays += it },
        )

        val result = stream.observe(RealtimeConnectRequest("wss://example.invalid/events")).toList()

        assertEquals(listOf("first", "second"), result.map { it.payload })
        assertEquals(listOf(10L, 20L), delays)
    }

    @Test
    fun failedConnectionsStopAfterConfiguredRetries() = runTest {
        var connects = 0
        val stream = RealtimeStream(
            transport = RealtimeTransport {
                connects += 1
                FailedConnection()
            },
            reconnectPolicy = RealtimeReconnectPolicy(maxReconnectAttempts = 2, initialDelayMillis = 0, maxDelayMillis = 0),
            retryDelay = RealtimeRetryDelay { },
        )

        assertEquals(emptyList(), stream.observe(RealtimeConnectRequest("wss://example.invalid/events")).toList())
        assertEquals(3, connects)
    }

    private class FakeConnection(messages: List<RealtimeMessage>) : RealtimeConnection {
        private val mutableState = MutableStateFlow<RealtimeConnectionState>(RealtimeConnectionState.Connected)
        override val state: StateFlow<RealtimeConnectionState> = mutableState
        override val incoming: Flow<RealtimeMessage> = flowOf(*messages.toTypedArray())
        override suspend fun send(message: RealtimeMessage) = Unit
        override suspend fun close() {
            mutableState.value = RealtimeConnectionState.Closed
        }
    }

    private class FailedConnection : RealtimeConnection {
        private val mutableState = MutableStateFlow<RealtimeConnectionState>(
            RealtimeConnectionState.Failed(RealtimeFailure.Transport),
        )
        override val state: StateFlow<RealtimeConnectionState> = mutableState
        override val incoming: Flow<RealtimeMessage> = flowOf()
        override suspend fun send(message: RealtimeMessage) = Unit
        override suspend fun close() {
            mutableState.value = RealtimeConnectionState.Closed
        }
    }
}
