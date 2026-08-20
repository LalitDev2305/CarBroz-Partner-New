package com.carbroz.partner.domain.session.operation

import com.carbroz.partner.domain.session.credential.CredentialPersistenceResult
import com.carbroz.partner.domain.session.credential.SessionCredentialPersistence
import com.carbroz.partner.domain.session.store.SessionStore

/**
 * Domain use case responsible for clearing durable credentials and marking SessionStore unauthenticated.
 */
public class ClearSession(
    private val credentialPersistence: SessionCredentialPersistence,
    private val sessionStore: SessionStore
) {
    public suspend fun execute(): ClearSessionResult {
        return when (credentialPersistence.clear()) {
            is CredentialPersistenceResult.Success -> {
                sessionStore.markUnauthenticated()
                ClearSessionResult.Cleared
            }
            is CredentialPersistenceResult.Failure -> {
                ClearSessionResult.PersistenceFailure
            }
        }
    }
}
