package com.carbroz.partner.shared

import com.carbroz.foundation.lifecycle.AppLifecycle
import com.carbroz.foundation.lifecycle.AppLifecycleController
import com.carbroz.foundation.lifecycle.AppLifecycleState
import com.carbroz.foundation.lifecycle.DefaultAppLifecycle

/**
 * Process-wide lifecycle bridge owned by the application host.
 *
 * Native and platform hosts report visibility transitions through the write-side
 * methods below. Common application/runtime code observes [lifecycle] and cannot
 * forge platform transitions.
 */
object AppLifecycleBridge {
    private val controller: AppLifecycleController = DefaultAppLifecycle()

    val lifecycle: AppLifecycle
        get() = controller

    fun moveToForeground() {
        controller.moveTo(AppLifecycleState.Foreground)
    }

    fun moveToBackground() {
        controller.moveTo(AppLifecycleState.Background)
    }
}
