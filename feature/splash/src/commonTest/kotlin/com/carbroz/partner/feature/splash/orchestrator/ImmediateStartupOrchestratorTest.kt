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
import kotlin.test.assertTrue

class ImmediateStartupOrchestratorTest {

    private class FakeCredentialPersistence : SessionCredentialPersistence {
        override suspend fun load(): CredentialLoadResult = CredentialLoadResult.NotFound
        override suspend fun save(credentials: SessionCredentials): CredentialPersistenceResult = CredentialPersistenceResult.Success
        override suspend fun clear(): CredentialPersistenceResult = CredentialPersistenceResult.Success
    }

    @Test
    fun testInitializeInvokesRestorerAndReturnsServerDrivenUi() = runTest {
        val persistence = FakeCredentialPersistence()
        val sessionStore = SessionStore()
        val restorer = SessionRestorer(persistence, sessionStore)
        val orchestrator = ImmediateStartupOrchestrator(
            sessionRestorer = restorer
        )

        val result = orchestrator.initialize()
        assertTrue(result is StartupResult.Ready)
        assertEquals(StartupDestination.ServerDrivenUi, (result as StartupResult.Ready).destination)
    }
}
