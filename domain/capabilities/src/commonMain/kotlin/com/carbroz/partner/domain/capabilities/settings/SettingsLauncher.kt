package com.carbroz.partner.domain.capabilities.settings

import com.carbroz.partner.domain.capabilities.core.CapabilityResult

/**
 * Domain launcher for opening system app settings page for permission recovery.
 */
interface SettingsLauncher {
    suspend fun openAppSettings(): CapabilityResult<Unit>
}
