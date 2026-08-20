package com.carbroz.partner.infrastructure.persistence.session

import com.carbroz.partner.domain.session.credential.SessionCredentialPersistence
import com.carbroz.partner.infrastructure.persistence.secure.DesktopSecureStorage

public object DesktopSessionCredentialPersistenceFactory {
    public fun create(): SessionCredentialPersistence {
        return DefaultSessionCredentialPersistence(DesktopSecureStorage())
    }
}
