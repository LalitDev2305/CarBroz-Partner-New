package com.carbroz.partner.domain.session.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
public data class SessionCredentials(
    @SerialName("access_token") val accessToken: String,
    @SerialName("refresh_token") val refreshToken: String? = null
) {
    override fun toString(): String {
        val hasRefresh = !refreshToken.isNullOrEmpty()
        return "SessionCredentials(hasAccessToken=${accessToken.isNotEmpty()}, hasRefreshToken=$hasRefresh)"
    }
}
