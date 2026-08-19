package com.carbroz.partner.feature.splash.orchestrator

import com.carbroz.partner.domain.session.restore.SessionRestorer

/**
 * Immediate, strategy-based implementation of [StartupOrchestrator] for bootstrap phase.
 *
 * Restores local session credentials and declares application readiness immediately,
 * returning [StartupDestination.ServerDrivenUi].
 */
public class ImmediateStartupOrchestrator(
    private val sessionRestorer: SessionRestorer? = null
) : StartupOrchestrator {

    override suspend fun initialize(): StartupResult {
        sessionRestorer?.restoreSession()
        return StartupResult.Ready(destination = StartupDestination.ServerDrivenUi)
    }
}
