package com.carbroz.partner.infrastructure.persistence.session

import com.carbroz.partner.domain.session.credential.SessionCredentialPersistence

public object DesktopSessionCredentialPersistenceFactory {
    public fun create(): SessionCredentialPersistence {
        return UnsupportedDesktopSessionCredentialPersistence
    }
}
