package com.carbroz.partner.infrastructure.persistence.session

import com.carbroz.partner.domain.session.credential.SessionCredentialStore
import com.carbroz.partner.infrastructure.persistence.secure.DesktopSecureStorage

public object DesktopSessionCredentialStoreFactory {
    public fun create(): SessionCredentialStore {
        return PersistentSessionCredentialStore(DesktopSecureStorage())
    }
}
