package com.carbroz.partner.infrastructure.persistence

import com.carbroz.partner.domain.storage.core.StorageResult
import com.carbroz.partner.infrastructure.persistence.secure.DesktopSecureStorage
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class DesktopSecureStorageTest {

    @Test
    fun getSecret_returnsNotFound_whenKeyDoesNotExist() = runTest {
        val storage = DesktopSecureStorage()
        val result = storage.getSecret("non_existent_key")
        assertIs<StorageResult.NotFound>(result)
    }

    @Test
    fun putSecret_storesValue_andGetSecretRetrievesIt() = runTest {
        val storage = DesktopSecureStorage()
        val putResult = storage.putSecret("key1", "secret_value_123")
        assertIs<StorageResult.Success<Unit>>(putResult)

        val getResult = storage.getSecret("key1")
        assertIs<StorageResult.Success<String>>(getResult)
        assertEquals("secret_value_123", getResult.value)
    }

    @Test
    fun removeSecret_removesStoredValue() = runTest {
        val storage = DesktopSecureStorage()
        storage.putSecret("key1", "secret_value_123")

        val removeResult = storage.removeSecret("key1")
        assertIs<StorageResult.Success<Unit>>(removeResult)

        val getResult = storage.getSecret("key1")
        assertIs<StorageResult.NotFound>(getResult)
    }
}
