package com.carbroz.partner.core.observability.model

/**
 * Functional contract providing epoch millisecond timestamps for log events.
 * This internal time contract avoids inventing a global application Clock inside Observability.
 */
fun interface Clock {
    fun nowEpochMilliseconds(): Long
}
