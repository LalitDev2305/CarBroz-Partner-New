package com.carbroz.foundation.lifecycle

/**
 * Write-side lifecycle port used only by platform/application adapters.
 *
 * UI, SDUI, data, and business code should depend on [AppLifecycle] instead so
 * platform lifecycle events cannot be forged from arbitrary consumers.
 */
interface AppLifecycleController : AppLifecycle {
    fun moveTo(state: AppLifecycleState)
}
