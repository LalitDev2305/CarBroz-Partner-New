package com.carbroz.partner.infrastructure.persistence.secure

import com.carbroz.partner.domain.storage.secure.SecureStorageGateway

/**
 * Multiplatform factory providing platform-specific target implementation of [SecureStorageGateway].
 */
public expect object SecureStorageFactory {
    public fun create(): SecureStorageGateway
}
