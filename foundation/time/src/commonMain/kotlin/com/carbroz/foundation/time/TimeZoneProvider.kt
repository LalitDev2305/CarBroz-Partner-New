package com.carbroz.foundation.time

/**
 * Supplies the current IANA time-zone identifier, for example `Asia/Kolkata`.
 *
 * Wall-clock time and time zone are intentionally separate concerns: changing
 * the device zone must never alter the underlying instant returned by [Clock].
 */
fun interface TimeZoneProvider {
    fun currentZoneId(): String
}
