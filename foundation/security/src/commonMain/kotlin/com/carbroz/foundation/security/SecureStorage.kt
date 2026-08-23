package com.carbroz.foundation.security

/**
 * Product-neutral secure key-value storage boundary for secrets and credentials.
 *
 * Platform implementations must use OS-backed secure storage where available
 * (for example Android Keystore-backed storage or iOS Keychain). Callers should
 * never place secrets in ordinary preferences or logs.
 */
interface SecureStorage {
    suspend fun read(key: SecureKey): String?
    suspend fun write(key: SecureKey, value: String)
    suspend fun remove(key: SecureKey)
    suspend fun clear()
}

/** Stable validated key used by [SecureStorage]. */
data class SecureKey(val value: String) {
    init {
        require(value.matches(Regex("^[a-z][a-z0-9_.-]{2,127}$"))) {
            "Secure key must be 3-128 lowercase characters using letters, digits, dot, underscore, or dash."
        }
    }
}
