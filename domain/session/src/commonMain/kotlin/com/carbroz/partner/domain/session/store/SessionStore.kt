package com.carbroz.partner.domain.session.store

import com.carbroz.partner.domain.session.model.SessionState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * State store managing application session state.
 */
public class SessionStore {

    private val _state = MutableStateFlow<SessionState>(SessionState.Unknown)
    public val state: StateFlow<SessionState> = _state.asStateFlow()

    public fun markAuthenticated() {
        _state.value = SessionState.Authenticated
    }

    public fun markUnauthenticated() {
        _state.value = SessionState.Unauthenticated
    }

    public fun markUnknown() {
        _state.value = SessionState.Unknown
    }
}
