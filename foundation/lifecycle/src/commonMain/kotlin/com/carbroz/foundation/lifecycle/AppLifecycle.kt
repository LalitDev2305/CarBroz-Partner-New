package com.carbroz.foundation.lifecycle

import kotlinx.coroutines.flow.StateFlow

/**
 * Read-only application lifecycle exposed to common foundation/runtime code.
 *
 * Consumers observe [state] but cannot drive lifecycle transitions. Platform
 * hosts own those transitions through [AppLifecycleController].
 */
interface AppLifecycle {
    val state: StateFlow<AppLifecycleState>
}
