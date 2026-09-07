package com.carbroz.data.network

import com.carbroz.foundation.session.SessionProvider
import com.carbroz.foundation.session.SessionRefreshCoordinator
import com.carbroz.foundation.session.SessionRefreshResult
import com.carbroz.foundation.session.SessionState
import com.carbroz.foundation.session.SessionStore
import com.carbroz.foundation.session.SessionTransitionResult
import com.carbroz.foundation.session.TokenRefreshFailure

/** Supplies transport-ready authorization without giving request authors control of the header. */
fun interface NetworkAuthorizationProvider {
    suspend fun authorizationHeader(): String?
}

object EmptyNetworkAuthorizationProvider : NetworkAuthorizationProvider {
    override suspend fun authorizationHeader(): String? = null
}

/** Semantic outcome of recovering from a server authentication rejection. */
sealed interface NetworkAuthenticationRecoveryResult {
    data object Recovered : NetworkAuthenticationRecoveryResult
    data object SessionInvalidated : NetworkAuthenticationRecoveryResult
    data object Unavailable : NetworkAuthenticationRecoveryResult
}

/** Coordinates recovery from a server authentication rejection. */
fun interface NetworkAuthenticationRecovery {
    suspend fun recover(): NetworkAuthenticationRecoveryResult
}

object NoNetworkAuthenticationRecovery : NetworkAuthenticationRecovery {
    override suspend fun recover(): NetworkAuthenticationRecoveryResult =
        NetworkAuthenticationRecoveryResult.Unavailable
}

class SessionNetworkAuthorizationProvider(
    private val sessionProvider: SessionProvider,
) : NetworkAuthorizationProvider {
    override suspend fun authorizationHeader(): String? {
        val authenticated = sessionProvider.current() as? SessionState.Authenticated ?: return null
        return "Bearer ${authenticated.tokens.accessToken.reveal()}"
    }
}

/**
 * Uses the canonical session refresh path and clears the canonical session only when credentials are
 * definitively unusable. Temporary refresh/network failures preserve the session for later recovery.
 */
class SessionNetworkAuthenticationRecovery(
    private val coordinator: SessionRefreshCoordinator,
    private val sessionStore: SessionStore,
) : NetworkAuthenticationRecovery {
    override suspend fun recover(): NetworkAuthenticationRecoveryResult =
        when (val result = coordinator.refreshAfterAuthenticationFailure()) {
            is SessionRefreshResult.Refreshed -> NetworkAuthenticationRecoveryResult.Recovered
            SessionRefreshResult.NotAuthenticated -> NetworkAuthenticationRecoveryResult.SessionInvalidated
            SessionRefreshResult.NotRequired,
            SessionRefreshResult.ExpiryUnknown,
            is SessionRefreshResult.ApplyFailed,
            -> NetworkAuthenticationRecoveryResult.Unavailable

            is SessionRefreshResult.Failed -> when (result.reason) {
                TokenRefreshFailure.MissingRefreshToken,
                TokenRefreshFailure.Rejected,
                TokenRefreshFailure.Unauthorized,
                TokenRefreshFailure.InvalidRefreshToken,
                -> invalidateSession()

                TokenRefreshFailure.NetworkUnavailable,
                TokenRefreshFailure.Unexpected,
                -> NetworkAuthenticationRecoveryResult.Unavailable
            }
        }

    private suspend fun invalidateSession(): NetworkAuthenticationRecoveryResult =
        when (sessionStore.signOut()) {
            is SessionTransitionResult.Success -> NetworkAuthenticationRecoveryResult.SessionInvalidated
            is SessionTransitionResult.Failed -> NetworkAuthenticationRecoveryResult.Unavailable
        }
}
