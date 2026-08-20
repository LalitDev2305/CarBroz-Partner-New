package com.carbroz.partner.infrastructure.persistence.secure

import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.alloc
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.value
import platform.CoreFoundation.CFDictionaryRef
import platform.CoreFoundation.kCFBooleanTrue
import platform.Foundation.CFBridgingRelease
import platform.Foundation.NSData
import platform.Foundation.NSString
import platform.Foundation.NSUTF8StringEncoding
import platform.Foundation.create
import platform.Foundation.dataUsingEncoding
import platform.Security.SecItemAdd
import platform.Security.SecItemCopyMatching
import platform.Security.SecItemDelete
import platform.Security.SecItemUpdate
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
 * iOS native Apple Keychain Services implementation of [SecureKeyValueStorage].
 */
@OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
internal class IosSecureStorage(
    private val serviceName: String = "com.carbroz.partner.secure"
) : SecureKeyValueStorage {

    override suspend fun read(key: String): SecureStorageResult {
        val query = mapOf(
            kSecClass to kSecClassGenericPassword,
            kSecAttrService to serviceName,
            kSecAttrAccount to key,
            kSecReturnData to kCFBooleanTrue,
            kSecMatchLimit to kSecMatchLimitOne
        )

        return memScoped {
            val resultPtr = alloc<platform.CoreFoundation.CFTypeRefVar>()
            val status = SecItemCopyMatching(query as CFDictionaryRef, resultPtr.ptr)
            if (status == errSecSuccess) {
                val nsData = CFBridgingRelease(resultPtr.value) as? NSData
                if (nsData != null) {
                    val stringPayload = NSString.create(data = nsData, encoding = NSUTF8StringEncoding)?.toString()
                    if (stringPayload != null) {
                        SecureStorageResult.Success(stringPayload)
                    } else {
                        SecureStorageResult.Failure
                    }
                } else {
                    SecureStorageResult.NotFound
                }
            } else if (status == errSecItemNotFound) {
                SecureStorageResult.NotFound
            } else {
                SecureStorageResult.Failure
            }
        }
    }

    override suspend fun write(key: String, value: String): SecureStorageResult {
        val nsData = (value as NSString).dataUsingEncoding(NSUTF8StringEncoding)
            ?: return SecureStorageResult.Failure

        val query = mapOf(
            kSecClass to kSecClassGenericPassword,
            kSecAttrService to serviceName,
            kSecAttrAccount to key
        )

        val updateFields = mapOf(
            kSecValueData to nsData,
            kSecAttrAccessible to kSecAttrAccessibleAfterFirstUnlockThisDeviceOnly
        )

        val updateStatus = SecItemUpdate(query as CFDictionaryRef, updateFields as CFDictionaryRef)
        if (updateStatus == errSecSuccess) {
            return SecureStorageResult.Success()
        }

        if (updateStatus == errSecItemNotFound) {
            val addQuery = query + mapOf(
                kSecValueData to nsData,
                kSecAttrAccessible to kSecAttrAccessibleAfterFirstUnlockThisDeviceOnly
            )
            val addStatus = SecItemAdd(addQuery as CFDictionaryRef, null)
            return if (addStatus == errSecSuccess) {
                SecureStorageResult.Success()
            } else {
                SecureStorageResult.Failure
            }
        }

        return SecureStorageResult.Failure
    }

    override suspend fun remove(key: String): SecureStorageResult {
        val query = mapOf(
            kSecClass to kSecClassGenericPassword,
            kSecAttrService to serviceName,
            kSecAttrAccount to key
        )

        val status = SecItemDelete(query as CFDictionaryRef)
        return if (status == errSecSuccess || status == errSecItemNotFound) {
            SecureStorageResult.Success()
        } else {
            SecureStorageResult.Failure
        }
    }
}
