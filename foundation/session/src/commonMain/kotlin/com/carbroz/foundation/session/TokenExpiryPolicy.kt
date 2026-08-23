package com.carbroz.foundation.session

import com.carbroz.foundation.time.Clock

/**
 * Decides whether an access token should be considered expired or close enough
 * to expiry that the session layer should refresh it before issuing work.
 *
 * The policy is deterministic because it depends on the shared [Clock]
 * abstraction rather than a platform clock.
 */
class TokenExpiryPolicy(
    private val clock: Clock,
    private val refreshSkewMilliseconds: Long = DEFAULT_REFRESH_SKEW_MILLISECONDS,
) {
    init {
        require(refreshSkewMilliseconds >= 0) { "Refresh skew must not be negative." }
    }

    fun evaluate(tokens: AuthTokens): TokenExpiryState {
        val expiresAt = tokens.accessTokenExpiresAtEpochMilliseconds
            ?: return TokenExpiryState.Unknown

        val now = clock.nowEpochMilliseconds()
        if (now >= expiresAt) return TokenExpiryState.Expired

        return if (expiresAt - now <= refreshSkewMilliseconds) {
            TokenExpiryState.RefreshRecommended
        } else {
            TokenExpiryState.Valid
        }
    }

    companion object {
        const val DEFAULT_REFRESH_SKEW_MILLISECONDS: Long = 60_000L
    }
}

/** Product-neutral interpretation of access-token lifetime. */
sealed interface TokenExpiryState {
    data object Valid : TokenExpiryState
    data object RefreshRecommended : TokenExpiryState
    data object Expired : TokenExpiryState
    data object Unknown : TokenExpiryState
}
