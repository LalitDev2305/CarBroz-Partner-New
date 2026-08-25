package com.carbroz.foundation.time

actual object SystemClock : Clock {
    actual override fun nowEpochMilliseconds(): Long = System.currentTimeMillis()
}
