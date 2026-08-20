package com.carbroz.partner.domain.session.credential

import com.carbroz.partner.domain.session.model.SessionCredentials

/**
 * Domain outcome for loading persisted session credentials.
 */
public sealed interface CredentialLoadResult {
    public data class Found(val credentials: SessionCredentials) : CredentialLoadResult
    public data object NotFound : CredentialLoadResult
    public data object Unavailable : CredentialLoadResult
}
