package com.carbroz.partner.feature.splash.orchestrator

/**
 * Immediate, strategy-based implementation of [StartupOrchestrator] for bootstrap phase.
 *
 * Declares application readiness immediately without remote network checks,
 * returning [StartupDestination.ServerDrivenUi].
 */
public class ImmediateStartupOrchestrator : StartupOrchestrator {

    override suspend fun initialize(): StartupResult {
        return StartupResult.Ready(destination = StartupDestination.ServerDrivenUi)
    }
}
