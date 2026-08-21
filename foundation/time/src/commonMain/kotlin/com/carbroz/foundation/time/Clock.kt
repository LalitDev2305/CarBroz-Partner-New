package com.carbroz.foundation.time

/**
 * Product-neutral source of wall-clock time.
 *
 * Business logic depends on this contract rather than platform clocks so time
 * dependent behavior remains deterministic in tests. Values are Unix epoch
 * milliseconds and therefore independent of locale and time zone.
 */
fun interface Clock {
    fun nowEpochMilliseconds(): Long
}

/**
 * Mutable deterministic clock intended for tests and controlled simulations.
 * Production platform clocks are supplied by platform composition roots.
 */
class MutableClock(
    initialEpochMilliseconds: Long,
) : Clock {
    private var current = initialEpochMilliseconds

    override fun nowEpochMilliseconds(): Long = current

    fun set(epochMilliseconds: Long) {
        current = epochMilliseconds
    }

    fun advanceBy(milliseconds: Long) {
        require(milliseconds >= 0L) { "Clock cannot be advanced by a negative duration." }
        require(current <= Long.MAX_VALUE - milliseconds) { "Clock advancement overflow." }
        current += milliseconds
    }
}
