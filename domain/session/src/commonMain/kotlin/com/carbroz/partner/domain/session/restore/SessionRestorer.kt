package com.carbroz.partner.domain.session.restore

import com.carbroz.partner.domain.session.credential.CredentialLoadResult
import com.carbroz.partner.domain.session.credential.SessionCredentialPersistence
import com.carbroz.partner.domain.session.store.SessionStore

/**
 * Domain use case responsible for restoring session state from persisted credentials at startup.
 */
public class SessionRestorer(
    private val credentialPersistence: SessionCredentialPersistence,
    private val sessionStore: SessionStore
) {
    public suspend fun restore(): SessionRestoreResult {
        return when (val result = credentialPersistence.load()) {
            is CredentialLoadResult.Found -> {
                sessionStore.markAuthenticated()
                SessionRestoreResult.Authenticated
            }
            is CredentialLoadResult.NotFound -> {
                sessionStore.markUnauthenticated()
                SessionRestoreResult.Unauthenticated
            }
            is CredentialLoadResult.Unavailable -> {
                // Persistence is unavailable; do NOT force SessionStore to Unauthenticated.
                SessionRestoreResult.Unavailable
            }
        }
    }
}
