package com.carbroz.partner.domain.session.credential

import com.carbroz.partner.domain.session.model.CredentialLoadResult
import com.carbroz.partner.domain.session.provider.SessionCredentialProvider

/**
 * [SessionCredentialProvider] implementation backed by [SessionCredentialPersistence].
 */
public class PersistedSessionCredentialProvider(
    private val credentialPersistence: SessionCredentialPersistence
) : SessionCredentialProvider {

    override suspend fun getAccessToken(): String? {
        return when (val result = credentialPersistence.load()) {
            is CredentialLoadResult.Found -> result.credentials.accessToken
            else -> null
        }
    }
}
