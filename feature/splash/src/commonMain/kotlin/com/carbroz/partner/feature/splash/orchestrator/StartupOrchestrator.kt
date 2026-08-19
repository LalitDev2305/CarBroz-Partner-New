package com.carbroz.partner.feature.splash.orchestrator

/**
 * Semantic startup destination determining which entry context the application should hand off to.
 */
public sealed interface StartupDestination {

    /**
     * Handoff destination indicating application is ready to enter the Server-Driven UI runtime.
     */
    public data object ServerDrivenUi : StartupDestination
}

/**
 * Semantic result of application startup orchestration.
 */
public sealed interface StartupResult {

    /**
     * Startup completed successfully with a target [StartupDestination].
     */
    public data class Ready(
        val destination: StartupDestination
    ) : StartupResult

    /**
     * Startup failed with a presentation-safe error message.
     */
    public data class Failure(
        val message: String
    ) : StartupResult
}

/**
 * Coordinates post-launch initialization and determines semantic application startup destination.
 */
public interface StartupOrchestrator {

    /**
     * Executes application startup checks and returns a [StartupResult].
     */
    public suspend fun initialize(): StartupResult
}
