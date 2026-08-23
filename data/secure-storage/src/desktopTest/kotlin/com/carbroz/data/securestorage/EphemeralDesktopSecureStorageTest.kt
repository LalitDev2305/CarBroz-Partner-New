package com.carbroz.data.securestorage

import com.carbroz.foundation.security.SecureKey
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class EphemeralDesktopSecureStorageTest {
    @Test
    fun storesRemovesAndClearsOnlyWithinProcessMemory() = runTest {
        val storage = EphemeralDesktopSecureStorage()
        val first = SecureKey("session.first")
        val second = SecureKey("session.second")

        storage.write(first, "one")
        storage.write(second, "two")
        assertEquals("one", storage.read(first))
        assertEquals("two", storage.read(second))

        storage.remove(first)
        assertNull(storage.read(first))

        storage.clear()
        assertNull(storage.read(second))
    }
}
