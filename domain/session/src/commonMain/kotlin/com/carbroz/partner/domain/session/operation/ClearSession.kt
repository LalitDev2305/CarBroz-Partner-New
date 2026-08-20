package com.carbroz.partner.domain.session.operation

import com.carbroz.partner.domain.session.credential.SessionCredentialStore
import com.carbroz.partner.domain.session.store.SessionStore

public class ClearSession(
    private val credentialStore: SessionCredentialStore,
    private val sessionStore: SessionStore
) {

    public suspend fun execute(): Boolean {
        val cleared = credentialStore.clear()
        sessionStore.markUnauthenticated()
        return cleared
    }
}
