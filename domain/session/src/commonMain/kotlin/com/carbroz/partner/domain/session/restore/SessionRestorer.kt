package com.carbroz.partner.domain.session.restore

import com.carbroz.partner.domain.session.credential.SessionCredentialStore
import com.carbroz.partner.domain.session.model.CredentialLoadResult
import com.carbroz.partner.domain.session.store.SessionStore

public class SessionRestorer(
    private val credentialStore: SessionCredentialStore,
    private val sessionStore: SessionStore
) {

    public suspend fun restoreSession() {
        when (credentialStore.load()) {
            is CredentialLoadResult.Found -> sessionStore.markAuthenticated()
            is CredentialLoadResult.NotFound -> sessionStore.markUnauthenticated()
            is CredentialLoadResult.Failure -> sessionStore.markUnauthenticated()
        }
    }
}
