package com.carbroz.partner.domain.capabilities.location

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class GeoCoordinateTest {

    @Test
    fun verifyValidCoordinates() {
        val center = GeoCoordinate(0.0, 0.0)
        val minBoundary = GeoCoordinate(-90.0, -180.0)
        val maxBoundary = GeoCoordinate(90.0, 180.0)

        assertEquals(0.0, center.latitude)
        assertEquals(0.0, center.longitude)
        assertEquals(-90.0, minBoundary.latitude)
        assertEquals(180.0, maxBoundary.longitude)
    }

    @Test
    fun verifyOutOfRangeLatitudeRejection() {
        assertFailsWith<IllegalArgumentException> { GeoCoordinate(-90.0001, 0.0) }
        assertFailsWith<IllegalArgumentException> { GeoCoordinate(90.0001, 0.0) }
    }

    @Test
    fun verifyOutOfRangeLongitudeRejection() {
        assertFailsWith<IllegalArgumentException> { GeoCoordinate(0.0, -180.0001) }
        assertFailsWith<IllegalArgumentException> { GeoCoordinate(0.0, 180.0001) }
    }

    @Test
    fun verifyNonFiniteLatitudeRejection() {
        assertFailsWith<IllegalArgumentException> { GeoCoordinate(Double.NaN, 0.0) }
        assertFailsWith<IllegalArgumentException> { GeoCoordinate(Double.POSITIVE_INFINITY, 0.0) }
        assertFailsWith<IllegalArgumentException> { GeoCoordinate(Double.NEGATIVE_INFINITY, 0.0) }
    }

    @Test
    fun verifyNonFiniteLongitudeRejection() {
        assertFailsWith<IllegalArgumentException> { GeoCoordinate(0.0, Double.NaN) }
        assertFailsWith<IllegalArgumentException> { GeoCoordinate(0.0, Double.POSITIVE_INFINITY) }
        assertFailsWith<IllegalArgumentException> { GeoCoordinate(0.0, Double.NEGATIVE_INFINITY) }
    }
}
