package com.carbroz.partner.domain.session.model

public sealed interface CredentialLoadResult {
    public data class Found(val credentials: SessionCredentials) : CredentialLoadResult
    public data object NotFound : CredentialLoadResult
    public data object Failure : CredentialLoadResult
}

public sealed interface SessionRefreshResult {
    public data class Success(val credentials: SessionCredentials) : SessionRefreshResult
    public data object Failure : SessionRefreshResult
}
