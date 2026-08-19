package com.carbroz.partner.domain.session

import com.carbroz.partner.domain.session.model.CredentialLoadResult
import com.carbroz.partner.domain.session.model.SessionCredentials
import com.carbroz.partner.domain.session.model.SessionRefreshResult
import com.carbroz.partner.domain.session.model.SessionState
import com.carbroz.partner.domain.session.operation.ClearSession
import com.carbroz.partner.domain.session.provider.StorageSessionCredentialProvider
import com.carbroz.partner.domain.session.refresh.SessionRefreshCoordinator
import com.carbroz.partner.domain.session.refresh.SessionRefreshGateway
import com.carbroz.partner.domain.session.restore.SessionRestorer
import com.carbroz.partner.domain.session.storage.SessionCredentialStorage
import com.carbroz.partner.domain.session.store.SessionStore
import com.carbroz.partner.domain.storage.core.StorageResult
import com.carbroz.partner.domain.storage.secure.SecureStorageGateway
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class DomainSessionTest {

    private class FakeSecureStorageGateway : SecureStorageGateway {
        val map = mutableMapOf<String, String>()

        override suspend fun getSecret(key: String): StorageResult<String> {
            val v = map[key] ?: return StorageResult.NotFound
            return StorageResult.Success(v)
        }

        override suspend fun putSecret(key: String, value: String): StorageResult<Unit> {
            map[key] = value
            return StorageResult.Success(Unit)
        }

        override suspend fun removeSecret(key: String): StorageResult<Unit> {
            map.remove(key)
            return StorageResult.Success(Unit)
        }
    }

    @Test
    fun testSessionStoreAndRestorer() = runTest {
        val storage = FakeSecureStorageGateway()
        val credStorage = SessionCredentialStorage(storage)
        val store = SessionStore()
        val restorer = SessionRestorer(credStorage, store)

        restorer.restoreSession()
        assertEquals(SessionState.Unauthenticated, store.state.value)

        val creds = SessionCredentials("token_123", "refresh_456")
        credStorage.saveCredentials(creds)

        restorer.restoreSession()
        assertEquals(SessionState.Authenticated, store.state.value)
    }

    @Test
    fun testCredentialProvider() = runTest {
        val storage = FakeSecureStorageGateway()
        val credStorage = SessionCredentialStorage(storage)
        val provider = StorageSessionCredentialProvider(credStorage)

        assertNull(provider.getAccessToken())

        credStorage.saveCredentials(SessionCredentials("tok_abc"))
        assertEquals("tok_abc", provider.getAccessToken())
    }

    @Test
    fun testSingleFlightConcurrentRefreshCoordinator() = runTest {
        val storage = FakeSecureStorageGateway()
        val credStorage = SessionCredentialStorage(storage)
        val store = SessionStore()
        val clear = ClearSession(credStorage, store)

        credStorage.saveCredentials(SessionCredentials("tok_old", "ref_old"))
        store.markAuthenticated()

        var gatewayCalls = 0
        val fakeGateway = SessionRefreshGateway { _ ->
            gatewayCalls++
            SessionRefreshResult.Success(SessionCredentials("tok_new", "ref_new"))
        }

        val coordinator = SessionRefreshCoordinator(credStorage, fakeGateway, clear)

        // Spawn 5 concurrent refresh requests using the exact same failed token "tok_old"
        val jobs = List(5) {
            async { coordinator.refresh("tok_old") }
        }
        val results = jobs.awaitAll()

        assertTrue(results.all { it })
        assertEquals(1, gatewayCalls, "Exactly ONE refresh call must reach the gateway for concurrent same-token 401s")
        assertEquals("tok_new", (credStorage.loadCredentials() as CredentialLoadResult.Found).credentials.accessToken)
    }

    @Test
    fun testClearSessionClearsCredentialsAndMarksUnauthenticated() = runTest {
        val storage = FakeSecureStorageGateway()
        val credStorage = SessionCredentialStorage(storage)
        val store = SessionStore()
        val clear = ClearSession(credStorage, store)

        credStorage.saveCredentials(SessionCredentials("tok_123"))
        store.markAuthenticated()

        clear.execute()
        assertEquals(SessionState.Unauthenticated, store.state.value)
        assertTrue(credStorage.loadCredentials() is CredentialLoadResult.NotFound)
    }
}
