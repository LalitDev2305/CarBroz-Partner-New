package com.carbroz.data.network

import com.carbroz.foundation.session.SessionProvider
import com.carbroz.foundation.session.SessionRefreshCoordinator
import com.carbroz.foundation.session.SessionRefreshResult
import com.carbroz.foundation.session.SessionState

/** Supplies transport-ready authorization without giving request authors control of the header. */
fun interface NetworkAuthorizationProvider {
    suspend fun authorizationHeader(): String?
}

object EmptyNetworkAuthorizationProvider : NetworkAuthorizationProvider {
    override suspend fun authorizationHeader(): String? = null
}

/** Coordinates recovery from a server authentication rejection. */
fun interface NetworkAuthenticationRecovery {
    suspend fun recover(): Boolean
}

object NoNetworkAuthenticationRecovery : NetworkAuthenticationRecovery {
    override suspend fun recover(): Boolean = false
}

class SessionNetworkAuthorizationProvider(
    private val sessionProvider: SessionProvider,
) : NetworkAuthorizationProvider {
    override suspend fun authorizationHeader(): String? {
        val authenticated = sessionProvider.current() as? SessionState.Authenticated ?: return null
        return "Bearer ${authenticated.tokens.accessToken.reveal()}"
    }
}

class SessionNetworkAuthenticationRecovery(
    private val coordinator: SessionRefreshCoordinator,
) : NetworkAuthenticationRecovery {
    override suspend fun recover(): Boolean =
        coordinator.refreshAfterAuthenticationFailure() is SessionRefreshResult.Refreshed
}
