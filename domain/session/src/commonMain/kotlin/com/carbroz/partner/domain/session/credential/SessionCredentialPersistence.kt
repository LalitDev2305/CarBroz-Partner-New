package com.carbroz.partner.domain.session.credential

import com.carbroz.partner.domain.session.model.SessionCredentials

/**
 * Domain port for durable session credential persistence.
 */
public interface SessionCredentialPersistence {
    public suspend fun load(): CredentialLoadResult
    public suspend fun save(credentials: SessionCredentials): CredentialPersistenceResult
    public suspend fun clear(): CredentialPersistenceResult
}
