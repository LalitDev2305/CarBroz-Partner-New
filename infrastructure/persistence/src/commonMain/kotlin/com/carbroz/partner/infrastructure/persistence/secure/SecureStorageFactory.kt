package com.carbroz.partner.infrastructure.persistence.secure

import com.carbroz.partner.domain.session.credential.SessionCredentialStore

/**
 * Multiplatform factory providing platform-specific target implementation of [SessionCredentialStore].
 */
public expect object SecureStorageFactory {
    public fun create(context: Any? = null): SessionCredentialStore
}
