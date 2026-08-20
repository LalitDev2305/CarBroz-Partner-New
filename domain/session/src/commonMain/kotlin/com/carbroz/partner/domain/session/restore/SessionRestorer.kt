package com.carbroz.partner.domain.session.restore

import com.carbroz.partner.domain.session.credential.SessionCredentialPersistence
import com.carbroz.partner.domain.session.model.CredentialLoadResult
import com.carbroz.partner.domain.session.store.SessionStore

public class SessionRestorer(
    private val credentialPersistence: SessionCredentialPersistence,
    private val sessionStore: SessionStore
) {
    public suspend fun restore(): Boolean {
        return when (val result = credentialPersistence.load()) {
            is CredentialLoadResult.Found -> {
                sessionStore.markAuthenticated()
                true
            }
            is CredentialLoadResult.NotFound -> {
                sessionStore.markUnauthenticated()
                false
            }
            is CredentialLoadResult.Failure -> {
                sessionStore.markUnauthenticated()
                false
            }
        }
    }
}
