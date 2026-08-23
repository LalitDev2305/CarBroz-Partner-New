package com.carbroz.foundation.session

import com.carbroz.foundation.security.Secret

/**
 * Authentication credentials held by the session layer.
 *
 * Tokens are wrapped in [Secret] so accidental logging/string interpolation
 * redacts them. Expiry is expressed as epoch milliseconds to remain platform neutral.
 */
data class AuthTokens(
    val accessToken: Secret,
    val refreshToken: Secret?,
    val accessTokenExpiresAtEpochMilliseconds: Long?,
)
