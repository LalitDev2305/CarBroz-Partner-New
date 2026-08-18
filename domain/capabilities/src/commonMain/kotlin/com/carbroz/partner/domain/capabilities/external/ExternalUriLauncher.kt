package com.carbroz.partner.domain.capabilities.external

import com.carbroz.partner.domain.capabilities.core.CapabilityResult

/**
 * Domain launcher for opening external URIs (e.g. web browser links).
 */
interface ExternalUriLauncher {
    suspend fun openUri(uri: String): CapabilityResult<Unit>
}
