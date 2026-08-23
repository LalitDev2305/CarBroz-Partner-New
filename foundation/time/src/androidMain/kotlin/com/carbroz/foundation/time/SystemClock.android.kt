package com.carbroz.foundation.time

actual object SystemClock : Clock {
    override fun nowEpochMilliseconds(): Long = System.currentTimeMillis()
}
