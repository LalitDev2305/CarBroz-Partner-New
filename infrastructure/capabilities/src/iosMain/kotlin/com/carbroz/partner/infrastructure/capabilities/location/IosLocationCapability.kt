package com.carbroz.partner.infrastructure.capabilities.location

import com.carbroz.partner.domain.capabilities.core.CapabilityResult
import com.carbroz.partner.domain.capabilities.location.LocationGateway
import com.carbroz.partner.domain.capabilities.location.LocationPrecision
import com.carbroz.partner.domain.capabilities.location.LocationSnapshot

/**
 * iOS native Apple implementation of [LocationGateway].
 */
public class IosLocationCapability : LocationGateway {

    override suspend fun getCurrentLocation(precision: LocationPrecision): CapabilityResult<LocationSnapshot> {
        return CapabilityResult.Unsupported(
            reason = "Location capability is not configured on iOS simulator environment"
        )
    }
}
