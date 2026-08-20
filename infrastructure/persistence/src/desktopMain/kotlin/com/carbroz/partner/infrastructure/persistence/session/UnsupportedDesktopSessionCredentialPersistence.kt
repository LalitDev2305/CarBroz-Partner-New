package com.carbroz.partner.infrastructure.persistence.session

import com.carbroz.partner.domain.session.credential.CredentialLoadResult
import com.carbroz.partner.domain.session.credential.CredentialPersistenceResult
import com.carbroz.partner.domain.session.credential.SessionCredentialPersistence
import com.carbroz.partner.domain.session.model.SessionCredentials

/**
 * Desktop secure credential persistence is intentionally unsupported until a
 * platform-appropriate secure persistent backend is provided.
 */
internal object UnsupportedDesktopSessionCredentialPersistence : SessionCredentialPersistence {
    override suspend fun load(): CredentialLoadResult = CredentialLoadResult.NotFound
    override suspend fun save(credentials: SessionCredentials): CredentialPersistenceResult = CredentialPersistenceResult.Failure
    override suspend fun clear(): CredentialPersistenceResult = CredentialPersistenceResult.Success
}
