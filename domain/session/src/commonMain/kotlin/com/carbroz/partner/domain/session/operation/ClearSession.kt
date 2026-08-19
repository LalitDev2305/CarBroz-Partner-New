package com.carbroz.partner.domain.session.operation

import com.carbroz.partner.domain.session.storage.SessionCredentialStorage
import com.carbroz.partner.domain.session.store.SessionStore

public class ClearSession(
    private val credentialStorage: SessionCredentialStorage,
    private val sessionStore: SessionStore
) {

    public suspend fun execute(): Boolean {
        val cleared = credentialStorage.clearCredentials()
        sessionStore.markUnauthenticated()
        return cleared
    }
}
