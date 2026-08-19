package com.carbroz.partner.domain.session.restore

import com.carbroz.partner.domain.session.model.CredentialLoadResult
import com.carbroz.partner.domain.session.storage.SessionCredentialStorage
import com.carbroz.partner.domain.session.store.SessionStore

public class SessionRestorer(
    private val credentialStorage: SessionCredentialStorage,
    private val sessionStore: SessionStore
) {

    public suspend fun restoreSession() {
        when (credentialStorage.loadCredentials()) {
            is CredentialLoadResult.Found -> sessionStore.markAuthenticated()
            is CredentialLoadResult.NotFound -> sessionStore.markUnauthenticated()
            is CredentialLoadResult.Failure -> sessionStore.markUnauthenticated()
        }
    }
}
