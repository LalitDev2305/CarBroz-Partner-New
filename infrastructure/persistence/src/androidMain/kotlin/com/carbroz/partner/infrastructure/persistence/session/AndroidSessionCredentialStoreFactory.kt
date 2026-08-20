package com.carbroz.partner.infrastructure.persistence.session

import android.content.Context
import com.carbroz.partner.domain.session.credential.SessionCredentialStore
import com.carbroz.partner.infrastructure.persistence.secure.AndroidSecureStorage

public object AndroidSessionCredentialStoreFactory {
    public fun create(context: Context): SessionCredentialStore {
        return PersistentSessionCredentialStore(
            AndroidSecureStorage(context.applicationContext)
        )
    }
}
