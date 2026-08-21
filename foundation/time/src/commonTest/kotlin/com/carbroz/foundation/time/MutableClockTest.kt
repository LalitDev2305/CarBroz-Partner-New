package com.carbroz.foundation.time

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class MutableClockTest {
    @Test
    fun `clock can be set and advanced deterministically`() {
        val clock = MutableClock(1_000L)

        clock.advanceBy(250L)
        assertEquals(1_250L, clock.nowEpochMilliseconds())

        clock.set(5_000L)
        assertEquals(5_000L, clock.nowEpochMilliseconds())
    }

    @Test
    fun `negative advancement is rejected`() {
        val clock = MutableClock(1_000L)
        assertFailsWith<IllegalArgumentException> { clock.advanceBy(-1L) }
        assertEquals(1_000L, clock.nowEpochMilliseconds())
    }

    @Test
    fun `overflow advancement is rejected without mutating time`() {
        val clock = MutableClock(Long.MAX_VALUE - 1L)
        assertFailsWith<IllegalArgumentException> { clock.advanceBy(2L) }
        assertEquals(Long.MAX_VALUE - 1L, clock.nowEpochMilliseconds())
    }
}
