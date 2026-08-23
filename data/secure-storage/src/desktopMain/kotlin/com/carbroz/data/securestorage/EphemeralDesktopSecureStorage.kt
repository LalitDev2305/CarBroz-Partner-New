package com.carbroz.data.securestorage

import com.carbroz.foundation.security.SecureKey
import com.carbroz.foundation.security.SecureStorage
import java.util.concurrent.ConcurrentHashMap

/**
 * Explicit Desktop secure-storage policy for the current foundation.
 *
 * Secrets are retained in process memory only and are never written to disk. This is a deliberate
 * fail-safe policy until a reviewed OS credential-vault adapter is selected for each supported
 * desktop operating system. The provider is fully functional for an authenticated process, but
 * sessions intentionally do not survive process termination.
 *
 * This class must not be replaced with plaintext preferences/files. Persistent Desktop credentials
 * require an OS-backed credential provider and an architecture update documenting that provider.
 */
class EphemeralDesktopSecureStorage : SecureStorage {
    private val values = ConcurrentHashMap<String, String>()

    override suspend fun read(key: SecureKey): String? = values[key.value]

    override suspend fun write(key: SecureKey, value: String) {
        values[key.value] = value
    }

    override suspend fun remove(key: SecureKey) {
        values.remove(key.value)
    }

    override suspend fun clear() {
        values.clear()
    }
}
