package com.carbroz.partner.domain.session.credential

import com.carbroz.partner.domain.session.model.CredentialLoadResult
import com.carbroz.partner.domain.session.model.SessionCredentials

/**
 * Domain boundary contract defining required credential persistence operations for authentication flows.
 */
public interface SessionCredentialStore {
    public suspend fun load(): CredentialLoadResult
    public suspend fun save(credentials: SessionCredentials): Boolean
    public suspend fun clear(): Boolean
}
