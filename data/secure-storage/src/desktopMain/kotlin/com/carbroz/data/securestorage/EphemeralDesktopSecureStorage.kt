package com.carbroz.data.securestorage

import com.carbroz.foundation.security.SecureKey
import com.carbroz.foundation.security.SecureStorage
import java.util.concurrent.ConcurrentHashMap

/**
 * Explicit production Desktop secure-storage policy.
 *
 * Credentials are process-ephemeral by design: secrets are retained in memory only and are never
 * written to disk, so Desktop sessions intentionally require authentication again after process
 * termination. This is a complete fail-closed policy, not a plaintext persistence fallback.
 *
 * If persistent Desktop credentials become a product requirement, they require an OS-backed
 * credential-vault provider and an architecture update; plaintext preferences/files are forbidden.
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
