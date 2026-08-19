package com.carbroz.partner.domain.session.store

import com.carbroz.partner.domain.session.model.SessionState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

public class SessionStore {

    private val _state = MutableStateFlow<SessionState>(SessionState.Unauthenticated)
    public val state: StateFlow<SessionState> = _state.asStateFlow()

    public fun markAuthenticated() {
        _state.value = SessionState.Authenticated
    }

    public fun markUnauthenticated() {
        _state.value = SessionState.Unauthenticated
    }
}
