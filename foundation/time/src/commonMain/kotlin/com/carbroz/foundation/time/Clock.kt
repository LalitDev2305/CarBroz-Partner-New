package com.carbroz.foundation.time

/**
 * Product-neutral source of wall-clock time.
 *
 * Business logic depends on this contract rather than platform clocks so time
 * dependent behavior remains deterministic when a test implementation is
 * supplied. Values are Unix epoch milliseconds and therefore independent of
 * locale and time zone.
 */
fun interface Clock {
    fun nowEpochMilliseconds(): Long
}

/**
 * Platform wall-clock implementation used by application composition roots.
 *
 * Test clocks intentionally live outside production source sets so the runtime
 * API does not expose mutable time controls.
 */
expect object SystemClock : Clock
