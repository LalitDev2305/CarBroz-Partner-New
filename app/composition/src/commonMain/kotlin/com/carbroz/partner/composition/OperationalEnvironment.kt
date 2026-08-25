package com.carbroz.partner.composition

import com.carbroz.foundation.configuration.AppEnvironment
import com.carbroz.foundation.observability.MainThreadDispatcher
import com.carbroz.foundation.observability.ObservabilitySinks
import com.carbroz.foundation.observability.ResourceDiagnostics

/** Local platform diagnostics selected after configuration validation. */
internal data class LocalOperationalDiagnostics(
    val sinks: ObservabilitySinks,
    val resourceDiagnostics: ResourceDiagnostics?,
    val mainThreadDispatcher: MainThreadDispatcher?,
)

/**
 * Development and staging may emit sanitized local platform diagnostics.
 * Production defaults to neutral/no-op sinks and no local watchdog/sampling; vendor adapters can be composed
 * through the same neutral sink contracts without changing runtime or data modules.
 */
internal fun localOperationalDiagnostics(
    environment: AppEnvironment,
    platformSinks: ObservabilitySinks,
    resourceDiagnostics: ResourceDiagnostics,
    mainThreadDispatcher: MainThreadDispatcher,
): LocalOperationalDiagnostics = when (environment) {
    AppEnvironment.Development,
    AppEnvironment.Staging,
    -> LocalOperationalDiagnostics(
        sinks = platformSinks,
        resourceDiagnostics = resourceDiagnostics,
        mainThreadDispatcher = mainThreadDispatcher,
    )

    AppEnvironment.Production -> LocalOperationalDiagnostics(
        sinks = ObservabilitySinks(),
        resourceDiagnostics = null,
        mainThreadDispatcher = null,
    )
}
