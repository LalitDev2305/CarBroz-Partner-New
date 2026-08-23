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

sealed interface TokenRefreshFailure {
    data object MissingRefreshToken : TokenRefreshFailure
    data class Rejected(val reason: String) : TokenRefreshFailure
    data class Unexpected(val reason: String) : TokenRefreshFailure
}
