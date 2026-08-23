package com.carbroz.partner.composition

import com.carbroz.foundation.lifecycle.AppLifecycle
import com.carbroz.foundation.lifecycle.AppLifecycleController
import com.carbroz.foundation.lifecycle.AppLifecycleState
import org.koin.core.Koin
import org.koin.mp.KoinPlatform

/**
 * Thin platform-to-common lifecycle bridge owned by the application composition boundary.
 *
 * Lifecycle state itself is owned by the Koin-composed [AppLifecycleController].
 * This bridge contains no parallel state and only translates host callbacks into
 * the shared semantic lifecycle contract.
 */
object AppLifecycleBridge {
    val lifecycle: AppLifecycle
        get() = koin().get()

    fun moveToForeground() {
        controller().moveTo(AppLifecycleState.Foreground)
    }

    fun moveToBackground() {
        controller().moveTo(AppLifecycleState.Background)
    }

    private fun controller(): AppLifecycleController = koin().get()

    private fun koin(): Koin = KoinPlatform.getKoinOrNull()
        ?: error("CarBroz dependency injection must be initialized before lifecycle events are forwarded.")
}
