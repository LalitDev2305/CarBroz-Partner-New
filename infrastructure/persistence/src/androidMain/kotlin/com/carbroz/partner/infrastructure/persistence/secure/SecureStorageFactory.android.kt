package com.carbroz.partner.infrastructure.persistence.secure

import android.content.Context
import com.carbroz.partner.domain.session.credential.SessionCredentialStore
import com.carbroz.partner.infrastructure.persistence.session.PersistentSessionCredentialStore

public actual object SecureStorageFactory {
    public actual fun create(context: Any?): SessionCredentialStore {
        return if (context is Context) {
            PersistentSessionCredentialStore(AndroidSecureStorage(context))
        } else {
            PersistentSessionCredentialStore(DesktopSecureStorage())
        }
    }
}
