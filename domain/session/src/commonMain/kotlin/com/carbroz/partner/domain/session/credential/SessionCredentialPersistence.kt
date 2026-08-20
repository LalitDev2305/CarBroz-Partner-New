package com.carbroz.partner.domain.session.credential

import com.carbroz.partner.domain.session.model.CredentialLoadResult
import com.carbroz.partner.domain.session.model.SessionCredentials

/**
 * Domain persistence port governing session credential storage and retrieval.
 */
public interface SessionCredentialPersistence {
    public suspend fun load(): CredentialLoadResult
    public suspend fun save(credentials: SessionCredentials): Boolean
    public suspend fun clear(): Boolean
}
