package com.carbroz.partner.domain.session.model

/**
 * Pure domain representation of session authentication credentials.
 */
public data class SessionCredentials(
    val accessToken: String,
    val refreshToken: String? = null
) {
    override fun toString(): String {
        val hasRefresh = !refreshToken.isNullOrEmpty()
        return "SessionCredentials(hasAccessToken=${accessToken.isNotEmpty()}, hasRefreshToken=$hasRefresh)"
    }
}
