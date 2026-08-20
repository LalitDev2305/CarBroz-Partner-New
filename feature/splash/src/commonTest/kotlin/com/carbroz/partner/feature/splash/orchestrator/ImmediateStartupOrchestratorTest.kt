package com.carbroz.partner.feature.splash.orchestrator

import com.carbroz.partner.domain.session.credential.CredentialLoadResult
import com.carbroz.partner.domain.session.credential.CredentialPersistenceResult
import com.carbroz.partner.domain.session.credential.SessionCredentialPersistence
import com.carbroz.partner.domain.session.model.SessionCredentials
import com.carbroz.partner.domain.session.restore.SessionRestorer
import com.carbroz.partner.domain.session.store.SessionStore
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ImmediateStartupOrchestratorTest {

    private class FakeCredentialPersistence(
        private val loadResult: CredentialLoadResult
    ) : SessionCredentialPersistence {
        override suspend fun load(): CredentialLoadResult = loadResult
        override suspend fun save(credentials: SessionCredentials): CredentialPersistenceResult = CredentialPersistenceResult.Success
        override suspend fun clear(): CredentialPersistenceResult = CredentialPersistenceResult.Success
    }

    @Test
    fun testInitializeAuthenticatedReturnsReadyServerDrivenUi() = runTest {
        val persistence = FakeCredentialPersistence(CredentialLoadResult.Found(SessionCredentials("tok_123")))
        val sessionStore = SessionStore()
        val restorer = SessionRestorer(persistence, sessionStore)
        val orchestrator = ImmediateStartupOrchestrator(restorer)

        val result = orchestrator.initialize()
        assertTrue(result is StartupResult.Ready)
        assertEquals(StartupDestination.ServerDrivenUi, result.destination)
    }

    @Test
    fun testInitializeUnauthenticatedReturnsReadyServerDrivenUi() = runTest {
        val persistence = FakeCredentialPersistence(CredentialLoadResult.NotFound)
        val sessionStore = SessionStore()
        val restorer = SessionRestorer(persistence, sessionStore)
        val orchestrator = ImmediateStartupOrchestrator(restorer)

        val result = orchestrator.initialize()
        assertTrue(result is StartupResult.Ready)
        assertEquals(StartupDestination.ServerDrivenUi, result.destination)
    }

    @Test
    fun testInitializeUnavailableReturnsFailureWithCleanMessage() = runTest {
        val persistence = FakeCredentialPersistence(CredentialLoadResult.Unavailable)
        val sessionStore = SessionStore()
        val restorer = SessionRestorer(persistence, sessionStore)
        val orchestrator = ImmediateStartupOrchestrator(restorer)

        val result = orchestrator.initialize()
        assertTrue(result is StartupResult.Failure)
        assertEquals("Unable to restore session", result.message)
        assertFalse(result.message.contains("Exception"))
        assertFalse(result.message.contains("Keychain"))
        assertFalse(result.message.contains("EncryptedSharedPreferences"))
    }
}
