package com.carbroz.partner.domain.capabilities.location

import com.carbroz.partner.domain.capabilities.core.CapabilityResult

/**
 * Domain gateway for acquiring one-shot location readings.
 */
interface LocationGateway {
    suspend fun getCurrentLocation(
        precision: LocationPrecision = LocationPrecision.PRECISE
    ): CapabilityResult<LocationSnapshot>
}
