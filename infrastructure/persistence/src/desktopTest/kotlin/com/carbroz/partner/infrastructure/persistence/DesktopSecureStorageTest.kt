package com.carbroz.partner.infrastructure.persistence

import com.carbroz.partner.domain.session.model.CredentialLoadResult
import com.carbroz.partner.domain.session.model.SessionCredentials
import com.carbroz.partner.infrastructure.persistence.secure.DesktopSecureStorage
import com.carbroz.partner.infrastructure.persistence.session.PersistentSessionCredentialStore
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class PersistentSessionCredentialStoreTest {

    @Test
    fun load_returnsNotFound_whenCredentialsDoNotExist() = runTest {
        val store = PersistentSessionCredentialStore(DesktopSecureStorage())
        val result = store.load()
        assertIs<CredentialLoadResult.NotFound>(result)
    }

    @Test
    fun save_storesCredentials_andLoadRetrievesThem() = runTest {
        val store = PersistentSessionCredentialStore(DesktopSecureStorage())
        val creds = SessionCredentials(accessToken = "access_123", refreshToken = "refresh_456")
        val saved = store.save(creds)
        assertTrue(saved)

        val loadResult = store.load()
        assertIs<CredentialLoadResult.Found>(loadResult)
        assertEquals(creds, loadResult.credentials)
    }

    @Test
    fun clear_removesStoredCredentials_andIsIdempotent() = runTest {
        val store = PersistentSessionCredentialStore(DesktopSecureStorage())
        val creds = SessionCredentials(accessToken = "access_123")

        store.save(creds)
        val clearedFirst = store.clear()
        assertTrue(clearedFirst)

        val loadResult = store.load()
        assertIs<CredentialLoadResult.NotFound>(loadResult)

        val clearedSecond = store.clear()
        assertTrue(clearedSecond, "Clear must be idempotent success when already missing")
    }

    @Test
    fun load_returnsFailure_whenPayloadIsCorrupted() = runTest {
        val desktopStorage = DesktopSecureStorage()
        val store = PersistentSessionCredentialStore(desktopStorage)

        desktopStorage.write(PersistentSessionCredentialStore.KEY_SESSION_CREDENTIALS, "{ invalid json }")

        val result = store.load()
        assertIs<CredentialLoadResult.Failure>(result)
    }
}
