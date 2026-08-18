package com.carbroz.partner.domain.capabilities.location

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class LocationSnapshotTest {

    @Test
    fun verifyValidSnapshot() {
        val coord = GeoCoordinate(12.9716, 77.5946)
        val snapshot = LocationSnapshot(coord, 5.0f, 1700000000000L)

        assertEquals(coord, snapshot.coordinate)
        assertEquals(5.0f, snapshot.accuracyMeters)
        assertEquals(1700000000000L, snapshot.timestampEpochMs)
    }

    @Test
    fun verifyZeroAccuracyAccepted() {
        val snapshot = LocationSnapshot(GeoCoordinate(0.0, 0.0), 0.0f, 100L)
        assertEquals(0.0f, snapshot.accuracyMeters)
    }

    @Test
    fun verifyNegativeAccuracyRejection() {
        assertFailsWith<IllegalArgumentException> {
            LocationSnapshot(GeoCoordinate(0.0, 0.0), -0.1f, 100L)
        }
    }

    @Test
    fun verifyNonFiniteAccuracyRejection() {
        assertFailsWith<IllegalArgumentException> {
            LocationSnapshot(GeoCoordinate(0.0, 0.0), Float.NaN, 100L)
        }
        assertFailsWith<IllegalArgumentException> {
            LocationSnapshot(GeoCoordinate(0.0, 0.0), Float.POSITIVE_INFINITY, 100L)
        }
        assertFailsWith<IllegalArgumentException> {
            LocationSnapshot(GeoCoordinate(0.0, 0.0), Float.NEGATIVE_INFINITY, 100L)
        }
    }

    @Test
    fun verifyInvalidTimestampRejection() {
        assertFailsWith<IllegalArgumentException> {
            LocationSnapshot(GeoCoordinate(0.0, 0.0), 5.0f, 0L)
        }
        assertFailsWith<IllegalArgumentException> {
            LocationSnapshot(GeoCoordinate(0.0, 0.0), 5.0f, -100L)
        }
    }
}
