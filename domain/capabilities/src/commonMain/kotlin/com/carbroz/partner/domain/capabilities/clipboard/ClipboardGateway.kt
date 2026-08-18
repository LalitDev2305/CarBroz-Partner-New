package com.carbroz.partner.domain.capabilities.clipboard

import com.carbroz.partner.domain.capabilities.core.CapabilityResult

/**
 * Domain gateway for copying text to system clipboard.
 */
interface ClipboardGateway {
    suspend fun copyText(text: String): CapabilityResult<Unit>
}
