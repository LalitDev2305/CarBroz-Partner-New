package com.carbroz.partner.infrastructure.persistence

import com.carbroz.partner.domain.session.model.CredentialLoadResult
import com.carbroz.partner.domain.session.model.SessionCredentials
import com.carbroz.partner.infrastructure.persistence.secure.DesktopSecureStorage
import com.carbroz.partner.infrastructure.persistence.session.DefaultSessionCredentialPersistence
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class DefaultSessionCredentialPersistenceTest {

    @Test
    fun load_returnsNotFound_whenCredentialsDoNotExist() = runTest {
        val persistence = DefaultSessionCredentialPersistence(DesktopSecureStorage())
        val result = persistence.load()
        assertIs<CredentialLoadResult.NotFound>(result)
    }

    @Test
    fun save_storesCredentials_andLoadRetrievesThem() = runTest {
        val persistence = DefaultSessionCredentialPersistence(DesktopSecureStorage())
        val creds = SessionCredentials(accessToken = "access_123", refreshToken = "refresh_456")
        val saved = persistence.save(creds)
        assertTrue(saved)

        val loadResult = persistence.load()
        assertIs<CredentialLoadResult.Found>(loadResult)
        assertEquals(creds, loadResult.credentials)
    }

    @Test
    fun clear_removesStoredCredentials_andIsIdempotent() = runTest {
        val persistence = DefaultSessionCredentialPersistence(DesktopSecureStorage())
        val creds = SessionCredentials(accessToken = "access_123")

        persistence.save(creds)
        val clearedFirst = persistence.clear()
        assertTrue(clearedFirst)

        val loadResult = persistence.load()
        assertIs<CredentialLoadResult.NotFound>(loadResult)

        val clearedSecond = persistence.clear()
        assertTrue(clearedSecond, "Clear must be idempotent success when already missing")
    }

    @Test
    fun load_returnsFailure_whenPayloadIsCorrupted() = runTest {
        val desktopStorage = DesktopSecureStorage()
        val persistence = DefaultSessionCredentialPersistence(desktopStorage)

        desktopStorage.write("session_credentials", "{ invalid json }")

        val result = persistence.load()
        assertIs<CredentialLoadResult.Failure>(result)
    }
}
