package com.carbroz.partner.infrastructure.capabilities.location

import com.carbroz.partner.domain.capabilities.core.CapabilityFailure
import com.carbroz.partner.domain.capabilities.core.CapabilityResult
import com.carbroz.partner.domain.capabilities.location.LocationGateway
import com.carbroz.partner.domain.capabilities.location.LocationPrecision
import com.carbroz.partner.domain.capabilities.location.LocationSnapshot

/**
 * Desktop JVM implementation of [LocationGateway].
 *
 * Standard desktop environments lack a native location provider API without external native libraries or IP geolocation services.
 * Explicitly returns [CapabilityResult.Unsupported] rather than crashing or faking location readings.
 */
public class DesktopLocationCapability : LocationGateway {

    override suspend fun getCurrentLocation(precision: LocationPrecision): CapabilityResult<LocationSnapshot> {
        return CapabilityResult.Unsupported(
            reason = "Location capability is not supported on Desktop platform"
        )
    }
}
