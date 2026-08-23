package com.carbroz.foundation.time

import platform.Foundation.NSDate
import platform.Foundation.timeIntervalSince1970

actual object SystemClock : Clock {
    override fun nowEpochMilliseconds(): Long =
        (NSDate().timeIntervalSince1970 * 1_000.0).toLong()
}
