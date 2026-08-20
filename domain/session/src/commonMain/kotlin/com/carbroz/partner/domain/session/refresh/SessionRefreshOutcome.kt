package com.carbroz.partner.domain.session.refresh

/**
 * Domain outcome returned by [SessionRefreshCoordinator] to network layer.
 */
public sealed interface SessionRefreshOutcome {
    public data object Refreshed : SessionRefreshOutcome
    public data object AlreadyRefreshed : SessionRefreshOutcome
    public data object Rejected : SessionRefreshOutcome
    public data object Unavailable : SessionRefreshOutcome
    public data object PersistenceFailure : SessionRefreshOutcome
}
