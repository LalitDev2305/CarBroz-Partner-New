package com.carbroz.partner.infrastructure.persistence.session

import android.content.Context
import com.carbroz.partner.domain.session.credential.SessionCredentialPersistence
import com.carbroz.partner.infrastructure.persistence.secure.AndroidSecureStorage

public object AndroidSessionCredentialPersistenceFactory {
    public fun create(context: Context): SessionCredentialPersistence {
        return DefaultSessionCredentialPersistence(
            AndroidSecureStorage(context.applicationContext)
        )
    }
}
