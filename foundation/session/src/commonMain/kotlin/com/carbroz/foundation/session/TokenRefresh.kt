package com.carbroz.foundation.session

/**
 * Product-neutral boundary for obtaining a refreshed token set.
 *
 * Implementations may call a remote authentication service, but this contract
 * deliberately contains no networking or platform-specific details.
 */
fun interface TokenRefresher {
    suspend fun refresh(current: AuthTokens): TokenRefreshResult
}

sealed interface TokenRefreshResult {
    data class Success(val tokens: AuthTokens) : TokenRefreshResult
    data class Failed(val reason: TokenRefreshFailure) : TokenRefreshResult
}

/**
 * Sanitized semantic failures for token refresh.
 *
 * Raw exception messages, response bodies, credentials, tokens and PII must
 * never cross this boundary. Transport-specific failures are mapped into these
 * categories by the owning network/auth adapter.
 */
sealed interface TokenRefreshFailure {
    data object MissingRefreshToken : TokenRefreshFailure
    data object Rejected : TokenRefreshFailure
    data object NetworkUnavailable : TokenRefreshFailure
    data object Unauthorized : TokenRefreshFailure
    data object InvalidRefreshToken : TokenRefreshFailure
    data object Unexpected : TokenRefreshFailure
}
