package com.carbroz.partner.feature.splash.orchestrator

import com.carbroz.partner.domain.session.restore.SessionRestoreResult
import com.carbroz.partner.domain.session.restore.SessionRestorer

/**
 * Immediate, strategy-based implementation of [StartupOrchestrator] for bootstrap phase.
 *
 * Restores local session credentials and declares application readiness immediately,
 * returning [StartupDestination.ServerDrivenUi] for Authenticated and Unauthenticated outcomes,
 * and returning [StartupResult.Failure] for Unavailable outcomes.
 */
public class ImmediateStartupOrchestrator(
    private val sessionRestorer: SessionRestorer
) : StartupOrchestrator {

    override suspend fun initialize(): StartupResult {
        return when (sessionRestorer.restore()) {
            is SessionRestoreResult.Authenticated, is SessionRestoreResult.Unauthenticated -> {
                StartupResult.Ready(destination = StartupDestination.ServerDrivenUi)
            }
            is SessionRestoreResult.Unavailable -> {
                StartupResult.Failure(message = "Unable to restore session")
            }
        }
    }
}
