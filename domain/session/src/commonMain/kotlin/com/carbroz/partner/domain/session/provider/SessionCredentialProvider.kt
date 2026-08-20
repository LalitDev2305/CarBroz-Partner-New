package com.carbroz.partner.domain.session.provider

public fun interface SessionCredentialProvider {
    public suspend fun getAccessToken(): String?
}
