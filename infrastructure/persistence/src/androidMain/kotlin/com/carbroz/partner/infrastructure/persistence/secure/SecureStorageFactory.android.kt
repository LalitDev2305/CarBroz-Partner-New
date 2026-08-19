package com.carbroz.partner.infrastructure.persistence.secure

import com.carbroz.partner.domain.storage.secure.SecureStorageGateway

public actual object SecureStorageFactory {
    public actual fun create(): SecureStorageGateway = DesktopSecureStorage()
}
