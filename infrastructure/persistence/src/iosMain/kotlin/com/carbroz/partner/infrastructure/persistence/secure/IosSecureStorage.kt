package com.carbroz.partner.infrastructure.persistence.secure

import com.carbroz.partner.domain.storage.core.StorageFailure
import com.carbroz.partner.domain.storage.core.StorageResult
import com.carbroz.partner.domain.storage.secure.SecureStorageGateway
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
 * iOS native Apple Keychain Services implementation of [SecureStorageGateway].
 *
 * Configured with [kSecAttrAccessibleAfterFirstUnlockThisDeviceOnly] for device-bounded secure token persistence.
 */
@OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
public class IosSecureStorage(
    private val serviceName: String = "com.carbroz.partner.secure"
) : SecureStorageGateway {

    override suspend fun getSecret(key: String): StorageResult<String> {
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
                        StorageResult.Success(stringPayload)
                    } else {
                        StorageResult.Failure(
                            StorageFailure(
                                code = StorageFailure.FailureCode.READ_FAILED,
                                message = "Failed to decode Keychain data to UTF-8 String"
                            )
                        )
                    }
                } else {
                    StorageResult.NotFound
                }
            } else if (status == errSecItemNotFound) {
                StorageResult.NotFound
            } else {
                StorageResult.Failure(
                    StorageFailure(
                        code = StorageFailure.FailureCode.READ_FAILED,
                        message = "Keychain read failed with OSStatus $status"
                    )
                )
            }
        }
    }

    override suspend fun putSecret(key: String, value: String): StorageResult<Unit> {
        val nsData = (value as NSString).dataUsingEncoding(NSUTF8StringEncoding)
            ?: return StorageResult.Failure(
                StorageFailure(
                    code = StorageFailure.FailureCode.WRITE_FAILED,
                    message = "Failed to encode String payload to NSData"
                )
            )

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
            return StorageResult.Success(Unit)
        }

        if (updateStatus == errSecItemNotFound) {
            val addQuery = query + mapOf(
                kSecValueData to nsData,
                kSecAttrAccessible to kSecAttrAccessibleAfterFirstUnlockThisDeviceOnly
            )
            val addStatus = SecItemAdd(addQuery as CFDictionaryRef, null)
            return if (addStatus == errSecSuccess) {
                StorageResult.Success(Unit)
            } else {
                StorageResult.Failure(
                    StorageFailure(
                        code = StorageFailure.FailureCode.WRITE_FAILED,
                        message = "Keychain write failed with OSStatus $addStatus"
                    )
                )
            }
        }

        return StorageResult.Failure(
            StorageFailure(
                code = StorageFailure.FailureCode.WRITE_FAILED,
                message = "Keychain update failed with OSStatus $updateStatus"
            )
        )
    }

    override suspend fun removeSecret(key: String): StorageResult<Unit> {
        val query = mapOf(
            kSecClass to kSecClassGenericPassword,
            kSecAttrService to serviceName,
            kSecAttrAccount to key
        )

        val status = SecItemDelete(query as CFDictionaryRef)
        return if (status == errSecSuccess || status == errSecItemNotFound) {
            StorageResult.Success(Unit)
        } else {
            StorageResult.Failure(
                StorageFailure(
                    code = StorageFailure.FailureCode.DELETE_FAILED,
                    message = "Keychain delete failed with OSStatus $status"
                )
            )
        }
    }
}
