package com.carbroz.partner.infrastructure.persistence.session

import com.carbroz.partner.domain.session.credential.SessionCredentialStore
import com.carbroz.partner.infrastructure.persistence.secure.IosSecureStorage

public object IosSessionCredentialStoreFactory {
    public fun create(): SessionCredentialStore {
        return PersistentSessionCredentialStore(IosSecureStorage())
    }
}
