package com.carbroz.partner.domain.session.provider

import com.carbroz.partner.domain.session.credential.SessionCredentialStore
import com.carbroz.partner.domain.session.model.CredentialLoadResult

public fun interface SessionCredentialProvider {
    public suspend fun getAccessToken(): String?
}

public class StorageSessionCredentialProvider(
    private val credentialStore: SessionCredentialStore
) : SessionCredentialProvider {

    override suspend fun getAccessToken(): String? {
        return when (val result = credentialStore.load()) {
            is CredentialLoadResult.Found -> result.credentials.accessToken
            else -> null
        }
    }
}
