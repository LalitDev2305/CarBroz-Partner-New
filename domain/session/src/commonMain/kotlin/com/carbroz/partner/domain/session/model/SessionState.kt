package com.carbroz.partner.domain.session.model

/**
 * Domain states representing application session authentication status.
 */
public sealed interface SessionState {
    public data object Unknown : SessionState
    public data object Unauthenticated : SessionState
    public data object Authenticated : SessionState
}
