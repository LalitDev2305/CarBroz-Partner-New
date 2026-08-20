package com.carbroz.partner.domain.session.credential

/**
 * Domain outcome for credential write operations (save and clear).
 */
public sealed interface CredentialPersistenceResult {
    public data object Success : CredentialPersistenceResult
    public data object Failure : CredentialPersistenceResult
}
