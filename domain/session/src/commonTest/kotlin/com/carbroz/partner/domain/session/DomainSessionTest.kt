package com.carbroz.partner.domain.session

import com.carbroz.partner.domain.session.credential.SessionCredentialStore
import com.carbroz.partner.domain.session.model.CredentialLoadResult
import com.carbroz.partner.domain.session.model.SessionCredentials
import com.carbroz.partner.domain.session.model.SessionRefreshResult
import com.carbroz.partner.domain.session.model.SessionState
import com.carbroz.partner.domain.session.operation.ClearSession
import com.carbroz.partner.domain.session.provider.StorageSessionCredentialProvider
import com.carbroz.partner.domain.session.refresh.SessionRefreshCoordinator
import com.carbroz.partner.domain.session.refresh.SessionRefreshGateway
import com.carbroz.partner.domain.session.restore.SessionRestorer
import com.carbroz.partner.domain.session.store.SessionStore
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class DomainSessionTest {

    private class FakeSessionCredentialStore : SessionCredentialStore {
        var storedCredentials: SessionCredentials? = null

        override suspend fun load(): CredentialLoadResult {
            val creds = storedCredentials ?: return CredentialLoadResult.NotFound
            return CredentialLoadResult.Found(creds)
        }

        override suspend fun save(credentials: SessionCredentials): Boolean {
            storedCredentials = credentials
            return true
        }

        override suspend fun clear(): Boolean {
            storedCredentials = null
            return true
        }
    }

    @Test
    fun testCredentialsToStringHidesSecrets() {
        val creds = SessionCredentials("secret_access_123", "secret_refresh_456")
        val str = creds.toString()
        assertFalse(str.contains("secret_access_123"))
        assertFalse(str.contains("secret_refresh_456"))
        assertTrue(str.contains("hasAccessToken=true"))
        assertTrue(str.contains("hasRefreshToken=true"))
    }

    @Test
    fun testSessionStoreAndRestorer() = runTest {
        val storeImpl = FakeSessionCredentialStore()
        val store = SessionStore()
        val restorer = SessionRestorer(storeImpl, store)

        restorer.restoreSession()
        assertEquals(SessionState.Unauthenticated, store.state.value)

        val creds = SessionCredentials("token_123", "refresh_456")
        storeImpl.save(creds)

        restorer.restoreSession()
        assertEquals(SessionState.Authenticated, store.state.value)
    }

    @Test
    fun testCredentialProvider() = runTest {
        val storeImpl = FakeSessionCredentialStore()
        val provider = StorageSessionCredentialProvider(storeImpl)

        assertNull(provider.getAccessToken())

        storeImpl.save(SessionCredentials("tok_abc"))
        assertEquals("tok_abc", provider.getAccessToken())
    }

    @Test
    fun testSingleFlightConcurrentRefreshCoordinator() = runTest {
        val storeImpl = FakeSessionCredentialStore()
        val store = SessionStore()
        val clear = ClearSession(storeImpl, store)

        storeImpl.save(SessionCredentials("tok_old", "ref_old"))
        store.markAuthenticated()

        var gatewayCalls = 0
        val fakeGateway = SessionRefreshGateway { _ ->
            gatewayCalls++
            SessionRefreshResult.Success(SessionCredentials("tok_new", "ref_new"))
        }

        val coordinator = SessionRefreshCoordinator(storeImpl, fakeGateway, clear)

        // Spawn 5 concurrent refresh requests using the exact same failed token "tok_old"
        val jobs = List(5) {
            async { coordinator.refresh("tok_old") }
        }
        val results = jobs.awaitAll()

        assertTrue(results.all { it })
        assertEquals(1, gatewayCalls, "Exactly ONE refresh call must reach the gateway for concurrent same-token 401s")
        assertEquals("tok_new", (storeImpl.load() as CredentialLoadResult.Found).credentials.accessToken)
    }

    @Test
    fun testClearSessionClearsCredentialsAndMarksUnauthenticated() = runTest {
        val storeImpl = FakeSessionCredentialStore()
        val store = SessionStore()
        val clear = ClearSession(storeImpl, store)

        storeImpl.save(SessionCredentials("tok_123"))
        store.markAuthenticated()

        clear.execute()
        assertEquals(SessionState.Unauthenticated, store.state.value)
        assertTrue(storeImpl.load() is CredentialLoadResult.NotFound)
    }
}
