package com.carbroz.partner.infrastructure.persistence.session

import com.carbroz.partner.domain.session.credential.SessionCredentialPersistence
import com.carbroz.partner.infrastructure.persistence.secure.IosSecureStorage

public object IosSessionCredentialPersistenceFactory {
    public fun create(): SessionCredentialPersistence {
        return DefaultSessionCredentialPersistence(IosSecureStorage())
    }
}
