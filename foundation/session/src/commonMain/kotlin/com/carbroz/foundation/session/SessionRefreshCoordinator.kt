package com.carbroz.foundation.session

import kotlinx.coroutines.CancellationException

/**
 * Coordinates refresh decisions, token refresh, and safe application through
 * the canonical [SessionStore].
 *
 * The coordinator owns no session state. It reads the current state, evaluates
 * token lifetime, delegates refresh work, and applies successful results only
 * against the token set that initiated the refresh. This preserves
 * [SessionStore] as the single canonical session owner.
 */
class SessionRefreshCoordinator(
    private val sessionStore: SessionStore,
    private val expiryPolicy: TokenExpiryPolicy,
    private val tokenRefresher: TokenRefresher,
) {
    suspend fun refreshIfNeeded(): SessionRefreshResult {
        val current = sessionStore.current() as? SessionState.Authenticated
            ?: return SessionRefreshResult.NotAuthenticated

        return when (expiryPolicy.evaluate(current.tokens)) {
            TokenExpiryState.Valid -> SessionRefreshResult.NotRequired
            TokenExpiryState.Unknown -> SessionRefreshResult.ExpiryUnknown
            TokenExpiryState.RefreshRecommended,
            TokenExpiryState.Expired,
            -> refresh(current.tokens)
        }
    }

    private suspend fun refresh(expectedTokens: AuthTokens): SessionRefreshResult {
        if (expectedTokens.refreshToken == null) {
            return SessionRefreshResult.Failed(TokenRefreshFailure.MissingRefreshToken)
        }

        return when (val refreshed = refreshSafely(expectedTokens)) {
            is TokenRefreshResult.Failed -> SessionRefreshResult.Failed(refreshed.reason)
            is TokenRefreshResult.Success -> when (
                val transition = sessionStore.updateTokens(expectedTokens, refreshed.tokens)
            ) {
                is SessionTransitionResult.Success -> SessionRefreshResult.Refreshed(refreshed.tokens)
                is SessionTransitionResult.Failed -> SessionRefreshResult.ApplyFailed(transition.reason)
            }
        }
    }

    private suspend fun refreshSafely(tokens: AuthTokens): TokenRefreshResult =
        try {
            tokenRefresher.refresh(tokens)
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (_: Throwable) {
            // Never surface Throwable.message here: it may contain response data,
            // credentials, identifiers or other sensitive implementation detail.
            TokenRefreshResult.Failed(TokenRefreshFailure.Unexpected)
        }
}

sealed interface SessionRefreshResult {
    data object NotAuthenticated : SessionRefreshResult
    data object NotRequired : SessionRefreshResult
    data object ExpiryUnknown : SessionRefreshResult
    data class Refreshed(val tokens: AuthTokens) : SessionRefreshResult
    data class Failed(val reason: TokenRefreshFailure) : SessionRefreshResult
    data class ApplyFailed(val reason: SessionTransitionFailure) : SessionRefreshResult
}
