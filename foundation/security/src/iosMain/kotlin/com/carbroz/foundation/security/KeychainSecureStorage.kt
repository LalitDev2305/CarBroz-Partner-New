package com.carbroz.foundation.security

import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.alloc
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.usePinned
import kotlinx.cinterop.value
import platform.CoreFoundation.CFDictionaryRef
import platform.CoreFoundation.CFTypeRefVar
import platform.Foundation.NSData
import platform.Foundation.NSString
import platform.Foundation.NSUTF8StringEncoding
import platform.Foundation.create
import platform.Security.SecItemAdd
import platform.Security.SecItemCopyMatching
import platform.Security.SecItemDelete
import platform.Security.SecItemUpdate
import platform.Security.errSecItemNotFound
import platform.Security.errSecSuccess
import platform.Security.kSecAttrAccessibleAfterFirstUnlockThisDeviceOnly
import platform.Security.kSecAttrAccessible
import platform.Security.kSecAttrAccount
import platform.Security.kSecAttrService
import platform.Security.kSecClass
import platform.Security.kSecClassGenericPassword
import platform.Security.kSecMatchLimit
import platform.Security.kSecMatchLimitOne
import platform.Security.kSecReturnData
import platform.Security.kSecValueData

/**
 * Apple Keychain-backed [SecureStorage].
 *
 * This is intentionally an iOS-only adapter. The storage contract and all
 * session/authentication behaviour remain in common Kotlin code.
 */
@OptIn(ExperimentalForeignApi::class)
class KeychainSecureStorage(
    private val service: String = "com.carbroz.partner",
) : SecureStorage {

    override suspend fun read(key: SecureKey): String? = memScoped {
        val result = alloc<CFTypeRefVar>()
        val status = SecItemCopyMatching(
            query(key, returnData = true),
            result.ptr,
        )

        when (status) {
            errSecSuccess -> {
                val data = result.value as? NSData
                    ?: error("Keychain returned an unexpected value for '${key.value}'.")
                NSString.create(data = data, encoding = NSUTF8StringEncoding)?.toString()
                    ?: error("Keychain value for '${key.value}' is not valid UTF-8.")
            }
            errSecItemNotFound -> null
            else -> keychainFailure("read", key, status)
        }
    }

    override suspend fun write(key: SecureKey, value: String) {
        val data = value.encodeToByteArray().toNSData()
        val updateStatus = SecItemUpdate(
            query(key),
            mapOf(kSecValueData to data) as CFDictionaryRef,
        )

        when (updateStatus) {
            errSecSuccess -> Unit
            errSecItemNotFound -> {
                val addStatus = SecItemAdd(
                    mapOf(
                        kSecClass to kSecClassGenericPassword,
                        kSecAttrService to service,
                        kSecAttrAccount to key.value,
                        kSecAttrAccessible to kSecAttrAccessibleAfterFirstUnlockThisDeviceOnly,
                        kSecValueData to data,
                    ) as CFDictionaryRef,
                    null,
                )
                if (addStatus != errSecSuccess) keychainFailure("write", key, addStatus)
            }
            else -> keychainFailure("write", key, updateStatus)
        }
    }

    override suspend fun remove(key: SecureKey) {
        val status = SecItemDelete(query(key))
        if (status != errSecSuccess && status != errSecItemNotFound) {
            keychainFailure("remove", key, status)
        }
    }

    override suspend fun clear() {
        val status = SecItemDelete(
            mapOf(
                kSecClass to kSecClassGenericPassword,
                kSecAttrService to service,
            ) as CFDictionaryRef,
        )
        if (status != errSecSuccess && status != errSecItemNotFound) {
            error("Keychain clear failed with OSStatus $status.")
        }
    }

    private fun query(key: SecureKey, returnData: Boolean = false): CFDictionaryRef {
        val values = mutableMapOf<Any?, Any?>(
            kSecClass to kSecClassGenericPassword,
            kSecAttrService to service,
            kSecAttrAccount to key.value,
        )
        if (returnData) {
            values[kSecReturnData] = true
            values[kSecMatchLimit] = kSecMatchLimitOne
        }
        @Suppress("UNCHECKED_CAST")
        return values as CFDictionaryRef
    }

    private fun keychainFailure(operation: String, key: SecureKey, status: Int): Nothing =
        error("Keychain $operation failed for '${key.value}' with OSStatus $status.")
}

@OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
private fun ByteArray.toNSData(): NSData =
    if (isEmpty()) {
        NSData()
    } else {
        usePinned { pinned ->
            NSData.create(bytes = pinned.addressOf(0), length = size.toULong())
        }
    }
