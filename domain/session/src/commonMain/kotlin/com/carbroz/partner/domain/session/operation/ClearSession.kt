package com.carbroz.partner.domain.session.operation

import com.carbroz.partner.domain.session.credential.SessionCredentialPersistence
import com.carbroz.partner.domain.session.store.SessionStore

public class ClearSession(
    private val credentialPersistence: SessionCredentialPersistence,
    private val sessionStore: SessionStore
) {
    public suspend fun execute(): Boolean {
        val cleared = credentialPersistence.clear()
        sessionStore.markUnauthenticated()
        return cleared
    }
}
