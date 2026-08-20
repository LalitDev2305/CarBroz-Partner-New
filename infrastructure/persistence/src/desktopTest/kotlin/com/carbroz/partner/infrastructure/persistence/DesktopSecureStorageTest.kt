package com.carbroz.partner.infrastructure.persistence

import com.carbroz.partner.domain.session.credential.SessionCredentialPersistence
import com.carbroz.partner.domain.session.model.CredentialLoadResult
import com.carbroz.partner.domain.session.model.SessionCredentials
import com.carbroz.partner.infrastructure.persistence.session.DesktopSessionCredentialPersistenceFactory
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

class DesktopSessionCredentialPersistenceTest {

    @Test
    fun factory_returnsSessionCredentialPersistence() {
        val persistence = DesktopSessionCredentialPersistenceFactory.create()
        assertIs<SessionCredentialPersistence>(persistence)
    }

    @Test
    fun load_returnsNotFound() = runTest {
        val persistence = DesktopSessionCredentialPersistenceFactory.create()
        val result = persistence.load()
        assertIs<CredentialLoadResult.NotFound>(result)
    }

    @Test
    fun save_returnsFalse_forUnsupportedDesktopPersistence() = runTest {
        val persistence = DesktopSessionCredentialPersistenceFactory.create()
        val creds = SessionCredentials(accessToken = "access_123", refreshToken = "refresh_456")
        val saved = persistence.save(creds)
        assertFalse(saved, "Desktop credential persistence must return false when save is unsupported")
    }

    @Test
    fun clear_returnsTrue() = runTest {
        val persistence = DesktopSessionCredentialPersistenceFactory.create()
        val cleared = persistence.clear()
        assertTrue(cleared, "Clear must be idempotent success")
    }

    @Test
    fun credentials_toString_doesNotLogRawSecrets() {
        val creds = SessionCredentials(accessToken = "secret_access_token", refreshToken = "secret_refresh_token")
        val str = creds.toString()
        assertFalse(str.contains("secret_access_token"))
        assertFalse(str.contains("secret_refresh_token"))
        assertEquals("SessionCredentials(hasAccessToken=true, hasRefreshToken=true)", str)
    }
}
