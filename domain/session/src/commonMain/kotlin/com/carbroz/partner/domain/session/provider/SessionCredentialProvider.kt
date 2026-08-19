package com.carbroz.partner.domain.session.provider

import com.carbroz.partner.domain.session.model.CredentialLoadResult
import com.carbroz.partner.domain.session.storage.SessionCredentialStorage

public fun interface SessionCredentialProvider {
    public suspend fun getAccessToken(): String?
}

public class StorageSessionCredentialProvider(
    private val credentialStorage: SessionCredentialStorage
) : SessionCredentialProvider {

    override suspend fun getAccessToken(): String? {
        return when (val result = credentialStorage.loadCredentials()) {
            is CredentialLoadResult.Found -> result.credentials.accessToken
            else -> null
        }
    }
}
