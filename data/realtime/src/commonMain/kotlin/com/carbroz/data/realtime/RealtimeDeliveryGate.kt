package com.carbroz.data.realtime

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Reusable delivery guard for protocols that expose stable event IDs and monotonically increasing
 * stream sequence numbers. Transport frames remain raw; protocol adapters supply this metadata.
 */
class RealtimeDeliveryGate(
    private val maxRememberedIds: Int = 512,
) {
    init {
        require(maxRememberedIds > 0) { "maxRememberedIds must be positive" }
    }

    private val mutex = Mutex()
    private val rememberedIds = linkedSetOf<String>()
    private val lastSequenceByStream = mutableMapOf<String, Long>()

    suspend fun accept(
        eventId: String,
        stream: String,
        sequence: Long,
    ): Boolean = mutex.withLock {
        require(eventId.isNotBlank()) { "eventId must not be blank" }
        require(stream.isNotBlank()) { "stream must not be blank" }
        require(sequence >= 0L) { "sequence must be non-negative" }

        if (eventId in rememberedIds) return@withLock false

        val lastSequence = lastSequenceByStream[stream]
        if (lastSequence != null && sequence <= lastSequence) return@withLock false

        rememberedIds += eventId
        while (rememberedIds.size > maxRememberedIds) {
            rememberedIds.remove(rememberedIds.first())
        }
        lastSequenceByStream[stream] = sequence
        true
    }

    suspend fun resetStream(stream: String) {
        require(stream.isNotBlank()) { "stream must not be blank" }
        mutex.withLock { lastSequenceByStream.remove(stream) }
    }

    suspend fun clear() {
        mutex.withLock {
            rememberedIds.clear()
            lastSequenceByStream.clear()
        }
    }
}
