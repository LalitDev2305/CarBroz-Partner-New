package com.carbroz.data.realtime

import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class RealtimeDeliveryGateTest {
    @Test
    fun duplicateIdsAndOutOfOrderSequencesAreRejected() = runTest {
        val gate = RealtimeDeliveryGate()

        assertTrue(gate.accept(eventId = "a", stream = "jobs", sequence = 1))
        assertFalse(gate.accept(eventId = "a", stream = "jobs", sequence = 2))
        assertFalse(gate.accept(eventId = "b", stream = "jobs", sequence = 1))
        assertTrue(gate.accept(eventId = "c", stream = "jobs", sequence = 2))
    }

    @Test
    fun streamsTrackOrderingIndependently() = runTest {
        val gate = RealtimeDeliveryGate()

        assertTrue(gate.accept("a", "jobs", 5))
        assertTrue(gate.accept("b", "messages", 1))
        assertFalse(gate.accept("c", "jobs", 4))
        assertTrue(gate.accept("d", "messages", 2))
    }

    @Test
    fun resetStreamAllowsFreshOrderingButDoesNotForgetDuplicateIds() = runTest {
        val gate = RealtimeDeliveryGate()

        assertTrue(gate.accept("a", "jobs", 10))
        gate.resetStream("jobs")

        assertTrue(gate.accept("b", "jobs", 1))
        assertFalse(gate.accept("a", "jobs", 2))
    }

    @Test
    fun rememberedIdsAreBounded() = runTest {
        val gate = RealtimeDeliveryGate(maxRememberedIds = 2)

        assertTrue(gate.accept("a", "a-stream", 1))
        assertTrue(gate.accept("b", "b-stream", 1))
        assertTrue(gate.accept("c", "c-stream", 1))
        assertTrue(gate.accept("a", "d-stream", 1))
    }
}
