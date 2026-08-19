package com.carbroz.partner.domain.session.model

public sealed interface SessionState {
    public data object Unauthenticated : SessionState
    public data object Authenticated : SessionState
}
