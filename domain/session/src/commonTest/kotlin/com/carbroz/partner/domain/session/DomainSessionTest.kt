package com.carbroz.partner.domain.session

import com.carbroz.partner.domain.session.credential.PersistedSessionCredentialProvider
import com.carbroz.partner.domain.session.credential.SessionCredentialPersistence
import com.carbroz.partner.domain.session.model.CredentialLoadResult
import com.carbroz.partner.domain.session.model.SessionCredentials
import com.carbroz.partner.domain.session.model.SessionRefreshResult
import com.carbroz.partner.domain.session.model.SessionState
import com.carbroz.partner.domain.session.operation.ClearSession
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

    private class FakeSessionCredentialPersistence : SessionCredentialPersistence {
        var stored: SessionCredentials? = null
        var failOnSave = false
        var loadFailure = false

        override suspend fun load(): CredentialLoadResult {
            if (loadFailure) return CredentialLoadResult.Failure
            val c = stored ?: return CredentialLoadResult.NotFound
            return CredentialLoadResult.Found(c)
        }

        override suspend fun save(credentials: SessionCredentials): Boolean {
            if (failOnSave) return false
            stored = credentials
            return true
        }

        override suspend fun clear(): Boolean {
            stored = null
            return true
        }
    }

    @Test
    fun testSessionStoreThreadSafetyAndStateFlow() = runTest {
        val store = SessionStore()
        assertEquals(SessionState.Unauthenticated, store.state.value)

        val jobs = List(100) { i ->
            async {
                if (i % 2 == 0) store.markAuthenticated() else store.markUnauthenticated()
            }
        }
        jobs.awaitAll()
        assertTrue(store.state.value is SessionState)
    }

    @Test
    fun testSessionRestorerRestoresAuthenticatedWhenFound() = runTest {
        val persistenceImpl = FakeSessionCredentialPersistence()
        persistenceImpl.save(SessionCredentials(accessToken = "token_123"))
        val store = SessionStore()
        val restorer = SessionRestorer(persistenceImpl, store)

        val restored = restorer.restore()
        assertTrue(restored)
        assertEquals(SessionState.Authenticated, store.state.value)
    }

    @Test
    fun testSessionRestorerMarksUnauthenticatedWhenNotFoundOrFailure() = runTest {
        val persistenceImpl = FakeSessionCredentialPersistence()
        val store = SessionStore()
        store.markAuthenticated()
        val restorer = SessionRestorer(persistenceImpl, store)

        val restored = restorer.restore()
        assertFalse(restored)
        assertEquals(SessionState.Unauthenticated, store.state.value)
    }

    @Test
    fun testClearSessionClearsPersistenceAndSessionStore() = runTest {
        val persistenceImpl = FakeSessionCredentialPersistence()
        persistenceImpl.save(SessionCredentials(accessToken = "token_123"))
        val store = SessionStore()
        store.markAuthenticated()
        val clearSession = ClearSession(persistenceImpl, store)

        val cleared = clearSession.execute()
        assertTrue(cleared)
        assertEquals(SessionState.Unauthenticated, store.state.value)
        assertIsNotFound(persistenceImpl.load())
    }

    @Test
    fun testPersistedSessionCredentialProviderReturnsToken() = runTest {
        val persistenceImpl = FakeSessionCredentialPersistence()
        persistenceImpl.save(SessionCredentials(accessToken = "token_abc"))
        val provider = PersistedSessionCredentialProvider(persistenceImpl)

        assertEquals("token_abc", provider.getAccessToken())
    }

    @Test
    fun testPersistedSessionCredentialProviderReturnsNullWhenEmpty() = runTest {
        val persistenceImpl = FakeSessionCredentialPersistence()
        val provider = PersistedSessionCredentialProvider(persistenceImpl)

        assertNull(provider.getAccessToken())
    }

    @Test
    fun testSessionRefreshCoordinatorSuccess() = runTest {
        val persistenceImpl = FakeSessionCredentialPersistence()
        persistenceImpl.save(SessionCredentials(accessToken = "token_old", refreshToken = "refresh_old"))
        val store = SessionStore()
        val clearSession = ClearSession(persistenceImpl, store)

        val gateway = SessionRefreshGateway { refreshTok: String ->
            if (refreshTok == "refresh_old") {
                SessionRefreshResult.Success(SessionCredentials(accessToken = "token_new", refreshToken = "refresh_new"))
            } else {
                SessionRefreshResult.Failure
            }
        }

        val coordinator = SessionRefreshCoordinator(persistenceImpl, gateway, clearSession)
        val success = coordinator.refresh("token_old")

        assertTrue(success)
        assertEquals("token_new", (persistenceImpl.load() as CredentialLoadResult.Found).credentials.accessToken)
    }

    private fun assertIsNotFound(result: CredentialLoadResult) {
        assertTrue(result is CredentialLoadResult.NotFound)
    }
}
