package com.carbroz.partner.sdui.render.runtime.snapshot

import kotlin.test.Test
import kotlin.test.assertEquals

class SduiRenderSnapshotTest {

    @Test
    fun testParentSignalVersionDefaultsToZero() {
        val snapshot = SduiRenderSnapshot()
        assertEquals(0L, snapshot.getParentSignalVersion("unknown_target"))
    }

    @Test
    fun testParentSignalVersionResolution() {
        val snapshot = SduiRenderSnapshot(
            parentSignalVersions = mapOf("timer_1" to 3L, "button_1" to 5L)
        )
        assertEquals(3L, snapshot.getParentSignalVersion("timer_1"))
        assertEquals(5L, snapshot.getParentSignalVersion("button_1"))
        assertEquals(0L, snapshot.getParentSignalVersion("other_target"))
    }

    @Test
    fun testImmutabilityPointInTimeState() {
        val snapshot1 = SduiRenderSnapshot(parentSignalVersions = mapOf("timer_1" to 1L))
        val snapshot2 = snapshot1.copy(parentSignalVersions = snapshot1.parentSignalVersions + ("timer_1" to 2L))

        assertEquals(1L, snapshot1.getParentSignalVersion("timer_1"))
        assertEquals(2L, snapshot2.getParentSignalVersion("timer_1"))
    }
}
