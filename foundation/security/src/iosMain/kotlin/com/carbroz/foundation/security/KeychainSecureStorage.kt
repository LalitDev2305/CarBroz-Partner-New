@file:Suppress("CAST_NEVER_SUCCEEDS")

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
import platform.Foundation.NSDictionary
import platform.Foundation.NSString
import platform.Foundation.NSUTF8StringEncoding
import platform.Foundation.create
import platform.Security.SecItemAdd
import platform.Security.SecItemCopyMatching
import platform.Security.SecItemDelete
import platform.Security.SecItemUpdate
import platform.Security.errSecDuplicateItem
import platform.Security.errSecItemNotFound
import platform.Security.errSecSuccess
import platform.Security.kSecAttrAccessible
import platform.Security.kSecAttrAccessibleAfterFirstUnlockThisDeviceOnly
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
 * The service namespace is supplied by the product composition root so this
 * foundation adapter remains product-neutral. Items use
 * `AfterFirstUnlockThisDeviceOnly`: they remain available to legitimate
 * background work after the device has been unlocked once, while staying bound
 * to the current device and excluded from migration/backup restoration.
 *
 * Only the unavoidable Apple storage adapter lives in `iosMain`; the contract
 * and all authentication/session policy remain in common Kotlin code.
 */
@OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
class KeychainSecureStorage(
    private val service: String,
) : SecureStorage {
    init {
        require(service.isNotBlank()) { "Keychain service must not be blank." }
        require(service == service.trim()) { "Keychain service must not contain surrounding whitespace." }
    }

    override suspend fun read(key: SecureKey): String? = memScoped {
        val result = alloc<CFTypeRefVar>()
        val status = SecItemCopyMatching(query(key, returnData = true), result.ptr)

        when (status) {
            errSecSuccess -> {
                val data = result.value as? NSData
                    ?: error("Keychain returned an unexpected value.")
                NSString.create(data = data, encoding = NSUTF8StringEncoding)?.toString()
                    ?: error("Keychain value is not valid UTF-8.")
            }
            errSecItemNotFound -> null
            else -> keychainFailure("read", status)
        }
    }

    override suspend fun write(key: SecureKey, value: String) {
        val data = value.encodeToByteArray().toNSData()
        when (val updateStatus = update(key, data)) {
            errSecSuccess -> Unit
            errSecItemNotFound -> {
                when (val addStatus = SecItemAdd(
                    dictionaryOf(
                        kSecClass to kSecClassGenericPassword,
                        kSecAttrService to service,
                        kSecAttrAccount to key.value,
                        kSecAttrAccessible to kSecAttrAccessibleAfterFirstUnlockThisDeviceOnly,
                        kSecValueData to data,
                    ),
                    null,
                )) {
                    errSecSuccess -> Unit
                    errSecDuplicateItem -> {
                        val retryStatus = update(key, data)
                        if (retryStatus != errSecSuccess) keychainFailure("write", retryStatus)
                    }
                    else -> keychainFailure("write", addStatus)
                }
            }
            else -> keychainFailure("write", updateStatus)
        }
    }

    override suspend fun remove(key: SecureKey) {
        val status = SecItemDelete(query(key))
        if (status != errSecSuccess && status != errSecItemNotFound) {
            keychainFailure("remove", status)
        }
    }

    override suspend fun clear() {
        val status = SecItemDelete(
            dictionaryOf(
                kSecClass to kSecClassGenericPassword,
                kSecAttrService to service,
            ),
        )
        if (status != errSecSuccess && status != errSecItemNotFound) {
            keychainFailure("clear", status)
        }
    }

    private fun update(key: SecureKey, data: NSData): Int = SecItemUpdate(
        query(key),
        dictionaryOf(kSecValueData to data),
    )

    private fun query(key: SecureKey, returnData: Boolean = false): CFDictionaryRef {
        val entries = mutableListOf<Pair<Any?, Any?>>(
            kSecClass to kSecClassGenericPassword,
            kSecAttrService to service,
            kSecAttrAccount to key.value,
        )
        if (returnData) {
            entries += kSecReturnData to true
            entries += kSecMatchLimit to kSecMatchLimitOne
        }
        return dictionaryOf(*entries.toTypedArray())
    }

    private fun keychainFailure(operation: String, status: Int): Nothing =
        error("Keychain $operation failed with OSStatus $status.")
}

@OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
private fun dictionaryOf(vararg entries: Pair<Any?, Any?>): CFDictionaryRef =
    entries.toMap() as NSDictionary as CFDictionaryRef

@OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
private fun ByteArray.toNSData(): NSData =
    if (isEmpty()) {
        NSData()
    } else {
        usePinned { pinned ->
            NSData.create(bytes = pinned.addressOf(0), length = size.toULong())
        }
    }
