package com.carbroz.foundation.session

/** Stable opaque session subject identifier. */
data class SessionSubject(val value: String) {
    init {
        require(value.isNotBlank()) { "Session subject must not be blank." }
        require(value.length <= 256) { "Session subject must not exceed 256 characters." }
    }
}

/** Product-neutral session state. */
sealed interface SessionState {
    data object SignedOut : SessionState

    data class Authenticated(
        val subject: SessionSubject,
        val tokens: AuthTokens,
    ) : SessionState
}

/** Read-only current session boundary. */
fun interface SessionProvider {
    suspend fun current(): SessionState
}
