package com.carbroz.partner.domain.session

import com.carbroz.partner.domain.session.credential.CredentialLoadResult
import com.carbroz.partner.domain.session.credential.CredentialPersistenceResult
import com.carbroz.partner.domain.session.credential.PersistedSessionCredentialProvider
import com.carbroz.partner.domain.session.credential.SessionCredentialPersistence
import com.carbroz.partner.domain.session.model.SessionCredentials
import com.carbroz.partner.domain.session.model.SessionState
import com.carbroz.partner.domain.session.operation.ClearSession
import com.carbroz.partner.domain.session.operation.ClearSessionResult
import com.carbroz.partner.domain.session.refresh.SessionRefreshCoordinator
import com.carbroz.partner.domain.session.refresh.SessionRefreshGateway
import com.carbroz.partner.domain.session.refresh.SessionRefreshOutcome
import com.carbroz.partner.domain.session.refresh.SessionRefreshResult
import com.carbroz.partner.domain.session.restore.SessionRestoreResult
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
        var failOnClear = false
        var loadResultOverride: CredentialLoadResult? = null
        var clearCalledCount = 0

        override suspend fun load(): CredentialLoadResult {
            loadResultOverride?.let { return it }
            val c = stored ?: return CredentialLoadResult.NotFound
            return CredentialLoadResult.Found(c)
        }

        override suspend fun save(credentials: SessionCredentials): CredentialPersistenceResult {
            if (failOnSave) return CredentialPersistenceResult.Failure
            stored = credentials
            return CredentialPersistenceResult.Success
        }

        override suspend fun clear(): CredentialPersistenceResult {
            clearCalledCount++
            if (failOnClear) return CredentialPersistenceResult.Failure
            stored = null
            return CredentialPersistenceResult.Success
        }
    }

    @Test
    fun testSessionStoreInitialUnknownAndTransitions() = runTest {
        val store = SessionStore()
        assertEquals(SessionState.Unknown, store.state.value)

        store.markAuthenticated()
        assertEquals(SessionState.Authenticated, store.state.value)

        store.markUnauthenticated()
        assertEquals(SessionState.Unauthenticated, store.state.value)

        store.markUnknown()
        assertEquals(SessionState.Unknown, store.state.value)
    }

    @Test
    fun testSessionRestorerFoundMarksAuthenticated() = runTest {
        val persistence = FakeSessionCredentialPersistence()
        persistence.save(SessionCredentials(accessToken = "token_123"))
        val store = SessionStore()
        val restorer = SessionRestorer(persistence, store)

        val result = restorer.restore()
        assertEquals(SessionRestoreResult.Authenticated, result)
        assertEquals(SessionState.Authenticated, store.state.value)
    }

    @Test
    fun testSessionRestorerNotFoundMarksUnauthenticated() = runTest {
        val persistence = FakeSessionCredentialPersistence()
        val store = SessionStore()
        store.markAuthenticated()
        val restorer = SessionRestorer(persistence, store)

        val result = restorer.restore()
        assertEquals(SessionRestoreResult.Unauthenticated, result)
        assertEquals(SessionState.Unauthenticated, store.state.value)
    }

    @Test
    fun testSessionRestorerUnavailablePreservesUnknownState() = runTest {
        val persistence = FakeSessionCredentialPersistence()
        persistence.loadResultOverride = CredentialLoadResult.Unavailable
        val store = SessionStore()
        val restorer = SessionRestorer(persistence, store)

        val result = restorer.restore()
        assertEquals(SessionRestoreResult.Unavailable, result)
        assertEquals(SessionState.Unknown, store.state.value)
    }

    @Test
    fun testClearSessionSuccessMarksUnauthenticated() = runTest {
        val persistence = FakeSessionCredentialPersistence()
        persistence.save(SessionCredentials(accessToken = "token_123"))
        val store = SessionStore()
        store.markAuthenticated()
        val clearSession = ClearSession(persistence, store)

        val result = clearSession.execute()
        assertEquals(ClearSessionResult.Cleared, result)
        assertEquals(SessionState.Unauthenticated, store.state.value)
        assertTrue(persistence.load() is CredentialLoadResult.NotFound)
    }

    @Test
    fun testClearSessionFailureDoesNotMarkUnauthenticated() = runTest {
        val persistence = FakeSessionCredentialPersistence()
        persistence.save(SessionCredentials(accessToken = "token_123"))
        persistence.failOnClear = true
        val store = SessionStore()
        store.markAuthenticated()
        val clearSession = ClearSession(persistence, store)

        val result = clearSession.execute()
        assertEquals(ClearSessionResult.PersistenceFailure, result)
        assertEquals(SessionState.Authenticated, store.state.value)
    }

    @Test
    fun testPersistedSessionCredentialProviderReturnsToken() = runTest {
        val persistence = FakeSessionCredentialPersistence()
        persistence.save(SessionCredentials(accessToken = "token_abc"))
        val provider = PersistedSessionCredentialProvider(persistence)

        assertEquals("token_abc", provider.getAccessToken())
    }

    @Test
    fun testPersistedSessionCredentialProviderReturnsNullWhenNotFoundOrUnavailable() = runTest {
        val persistence = FakeSessionCredentialPersistence()
        val provider = PersistedSessionCredentialProvider(persistence)

        assertNull(provider.getAccessToken())

        persistence.loadResultOverride = CredentialLoadResult.Unavailable
        assertNull(provider.getAccessToken())
    }

    @Test
    fun testSessionRefreshCoordinatorLoadNotFoundMarksUnauthenticatedAndRejectsWithoutClearOrGatewayCall() = runTest {
        val persistence = FakeSessionCredentialPersistence()
        val store = SessionStore()
        store.markAuthenticated()
        val clearSession = ClearSession(persistence, store)

        var gatewayCalled = false
        val gateway = SessionRefreshGateway {
            gatewayCalled = true
            SessionRefreshResult.Rejected
        }

        val coordinator = SessionRefreshCoordinator(persistence, gateway, clearSession, store)
        val outcome = coordinator.refresh("token_old")

        assertEquals(SessionRefreshOutcome.Rejected, outcome)
        assertEquals(SessionState.Unauthenticated, store.state.value)
        assertFalse(gatewayCalled)
        assertEquals(0, persistence.clearCalledCount)
    }

    @Test
    fun testSessionRefreshCoordinatorLoadUnavailableReturnsUnavailableWithoutClearOrGatewayCallOrStoreMutation() = runTest {
        val persistence = FakeSessionCredentialPersistence()
        persistence.loadResultOverride = CredentialLoadResult.Unavailable
        val store = SessionStore()
        store.markAuthenticated()
        val clearSession = ClearSession(persistence, store)

        var gatewayCalled = false
        val gateway = SessionRefreshGateway {
            gatewayCalled = true
            SessionRefreshResult.Rejected
        }

        val coordinator = SessionRefreshCoordinator(persistence, gateway, clearSession, store)
        val outcome = coordinator.refresh("token_old")

        assertEquals(SessionRefreshOutcome.Unavailable, outcome)
        assertEquals(SessionState.Authenticated, store.state.value)
        assertFalse(gatewayCalled)
        assertEquals(0, persistence.clearCalledCount)
    }

    @Test
    fun testSessionRefreshCoordinatorSuccess() = runTest {
        val persistence = FakeSessionCredentialPersistence()
        persistence.save(SessionCredentials(accessToken = "token_old", refreshToken = "refresh_old"))
        val store = SessionStore()
        val clearSession = ClearSession(persistence, store)

        val gateway = SessionRefreshGateway { refreshTok ->
            if (refreshTok == "refresh_old") {
                SessionRefreshResult.Success(SessionCredentials(accessToken = "token_new", refreshToken = "refresh_new"))
            } else {
                SessionRefreshResult.Rejected
            }
        }

        val coordinator = SessionRefreshCoordinator(persistence, gateway, clearSession, store)
        val outcome = coordinator.refresh("token_old")

        assertEquals(SessionRefreshOutcome.Refreshed, outcome)
        assertEquals(SessionState.Authenticated, store.state.value)
        assertEquals("token_new", (persistence.load() as CredentialLoadResult.Found).credentials.accessToken)
    }

    @Test
    fun testSessionRefreshCoordinatorAlreadyRefreshed() = runTest {
        val persistence = FakeSessionCredentialPersistence()
        persistence.save(SessionCredentials(accessToken = "token_new", refreshToken = "refresh_new"))
        val store = SessionStore()
        val clearSession = ClearSession(persistence, store)
        val gateway = SessionRefreshGateway { SessionRefreshResult.Rejected }

        val coordinator = SessionRefreshCoordinator(persistence, gateway, clearSession, store)
        val outcome = coordinator.refresh("token_old")

        assertEquals(SessionRefreshOutcome.AlreadyRefreshed, outcome)
        assertEquals(SessionState.Authenticated, store.state.value)
    }

    @Test
    fun testSessionRefreshCoordinatorMissingRefreshTokenClearSuccessReturnsRejected() = runTest {
        val persistence = FakeSessionCredentialPersistence()
        persistence.save(SessionCredentials(accessToken = "token_old", refreshToken = null))
        val store = SessionStore()
        val clearSession = ClearSession(persistence, store)
        val gateway = SessionRefreshGateway { SessionRefreshResult.Rejected }

        val coordinator = SessionRefreshCoordinator(persistence, gateway, clearSession, store)
        val outcome = coordinator.refresh("token_old")

        assertEquals(SessionRefreshOutcome.Rejected, outcome)
        assertEquals(SessionState.Unauthenticated, store.state.value)
        assertEquals(1, persistence.clearCalledCount)
    }

    @Test
    fun testSessionRefreshCoordinatorMissingRefreshTokenClearFailureReturnsPersistenceFailure() = runTest {
        val persistence = FakeSessionCredentialPersistence()
        persistence.save(SessionCredentials(accessToken = "token_old", refreshToken = null))
        persistence.failOnClear = true
        val store = SessionStore()
        store.markAuthenticated()
        val clearSession = ClearSession(persistence, store)
        val gateway = SessionRefreshGateway { SessionRefreshResult.Rejected }

        val coordinator = SessionRefreshCoordinator(persistence, gateway, clearSession, store)
        val outcome = coordinator.refresh("token_old")

        assertEquals(SessionRefreshOutcome.PersistenceFailure, outcome)
        assertEquals(SessionState.Authenticated, store.state.value)
        assertEquals(1, persistence.clearCalledCount)
    }

    @Test
    fun testSessionRefreshCoordinatorRemoteRejectedClearSuccessReturnsRejected() = runTest {
        val persistence = FakeSessionCredentialPersistence()
        persistence.save(SessionCredentials(accessToken = "token_old", refreshToken = "refresh_old"))
        val store = SessionStore()
        val clearSession = ClearSession(persistence, store)
        val gateway = SessionRefreshGateway { SessionRefreshResult.Rejected }

        val coordinator = SessionRefreshCoordinator(persistence, gateway, clearSession, store)
        val outcome = coordinator.refresh("token_old")

        assertEquals(SessionRefreshOutcome.Rejected, outcome)
        assertEquals(SessionState.Unauthenticated, store.state.value)
        assertEquals(1, persistence.clearCalledCount)
    }

    @Test
    fun testSessionRefreshCoordinatorRemoteRejectedClearFailureReturnsPersistenceFailure() = runTest {
        val persistence = FakeSessionCredentialPersistence()
        persistence.save(SessionCredentials(accessToken = "token_old", refreshToken = "refresh_old"))
        persistence.failOnClear = true
        val store = SessionStore()
        store.markAuthenticated()
        val clearSession = ClearSession(persistence, store)
        val gateway = SessionRefreshGateway { SessionRefreshResult.Rejected }

        val coordinator = SessionRefreshCoordinator(persistence, gateway, clearSession, store)
        val outcome = coordinator.refresh("token_old")

        assertEquals(SessionRefreshOutcome.PersistenceFailure, outcome)
        assertEquals(SessionState.Authenticated, store.state.value)
        assertEquals(1, persistence.clearCalledCount)
    }

    @Test
    fun testSessionRefreshCoordinatorTransientUnavailablePreservesSession() = runTest {
        val persistence = FakeSessionCredentialPersistence()
        persistence.save(SessionCredentials(accessToken = "token_old", refreshToken = "refresh_old"))
        val store = SessionStore()
        store.markAuthenticated()
        val clearSession = ClearSession(persistence, store)

        val gateway = SessionRefreshGateway { SessionRefreshResult.Unavailable }

        val coordinator = SessionRefreshCoordinator(persistence, gateway, clearSession, store)
        val outcome = coordinator.refresh("token_old")

        assertEquals(SessionRefreshOutcome.Unavailable, outcome)
        assertEquals(SessionState.Authenticated, store.state.value)
        assertTrue(persistence.load() is CredentialLoadResult.Found)
        assertEquals(0, persistence.clearCalledCount)
    }

    @Test
    fun testSessionRefreshCoordinatorPersistenceFailureDoesNotMarkAuthenticated() = runTest {
        val persistence = FakeSessionCredentialPersistence()
        persistence.save(SessionCredentials(accessToken = "token_old", refreshToken = "refresh_old"))
        persistence.failOnSave = true
        val store = SessionStore()
        val clearSession = ClearSession(persistence, store)

        val gateway = SessionRefreshGateway { _ ->
            SessionRefreshResult.Success(SessionCredentials(accessToken = "token_new", refreshToken = "refresh_new"))
        }

        val coordinator = SessionRefreshCoordinator(persistence, gateway, clearSession, store)
        val outcome = coordinator.refresh("token_old")

        assertEquals(SessionRefreshOutcome.PersistenceFailure, outcome)
        assertEquals(SessionState.Unauthenticated, store.state.value)
    }

    @Test
    fun testSessionCredentialsToStringRedactsTokens() = runTest {
        val credentials = SessionCredentials(accessToken = "secret_access_123", refreshToken = "secret_refresh_456")
        val str = credentials.toString()

        assertFalse(str.contains("secret_access_123"))
        assertFalse(str.contains("secret_refresh_456"))
        assertTrue(str.contains("hasAccessToken=true"))
        assertTrue(str.contains("hasRefreshToken=true"))
    }
}
