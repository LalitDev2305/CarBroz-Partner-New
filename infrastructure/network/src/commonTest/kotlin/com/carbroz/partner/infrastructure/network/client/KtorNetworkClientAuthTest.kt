package com.carbroz.partner.infrastructure.network.client

import com.carbroz.partner.domain.session.credential.CredentialLoadResult
import com.carbroz.partner.domain.session.credential.CredentialPersistenceResult
import com.carbroz.partner.domain.session.credential.SessionCredentialPersistence
import com.carbroz.partner.domain.session.model.SessionCredentials
import com.carbroz.partner.domain.session.operation.ClearSession
import com.carbroz.partner.domain.session.provider.SessionCredentialProvider
import com.carbroz.partner.domain.session.refresh.SessionRefreshCoordinator
import com.carbroz.partner.domain.session.refresh.SessionRefreshGateway
import com.carbroz.partner.domain.session.refresh.SessionRefreshResult
import com.carbroz.partner.domain.session.store.SessionStore
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class KtorNetworkClientAuthTest {

    private class FakeCredentialPersistence : SessionCredentialPersistence {
        var stored: SessionCredentials? = null
        override suspend fun load(): CredentialLoadResult {
            val c = stored ?: return CredentialLoadResult.NotFound
            return CredentialLoadResult.Found(c)
        }
        override suspend fun save(credentials: SessionCredentials): CredentialPersistenceResult {
            stored = credentials
            return CredentialPersistenceResult.Success
        }
        override suspend fun clear(): CredentialPersistenceResult {
            stored = null
            return CredentialPersistenceResult.Success
        }
    }

    @Test
    fun testAuthPolicyNoneBypassesAuthHeader() = runTest {
        var capturedAuthHeader: String? = null
        val mockEngine = MockEngine { req ->
            capturedAuthHeader = req.headers["Authorization"]
            respond("{}", HttpStatusCode.OK)
        }
        val provider = SessionCredentialProvider { "tok_123" }
        val client = KtorNetworkClient(
            baseUrl = "https://api.test.com",
            credentialProvider = provider,
            baseHttpClient = HttpClient(mockEngine)
        )

        val resp = client.execute(NetworkRequest(url = "/public", authPolicy = AuthPolicy.NONE))
        assertTrue(resp.isSuccessful)
        assertNull(capturedAuthHeader, "AuthPolicy.NONE must not attach Authorization header")
    }

    @Test
    fun testAuthPolicyOptionalAttachesTokenWhenPresent() = runTest {
        var capturedAuthHeader: String? = null
        val mockEngine = MockEngine { req ->
            capturedAuthHeader = req.headers["Authorization"]
            respond("{}", HttpStatusCode.OK)
        }
        val provider = SessionCredentialProvider { "tok_123" }
        val client = KtorNetworkClient(
            baseUrl = "https://api.test.com",
            credentialProvider = provider,
            baseHttpClient = HttpClient(mockEngine)
        )

        val resp = client.execute(NetworkRequest(url = "/api/v1/sdui/root", authPolicy = AuthPolicy.OPTIONAL))
        assertTrue(resp.isSuccessful)
        assertEquals("Bearer tok_123", capturedAuthHeader)
    }

    @Test
    fun testAuthPolicyRequiredFailsLocallyWhenNoToken() = runTest {
        var httpExecuted = false
        val mockEngine = MockEngine { _ ->
            httpExecuted = true
            respond("{}", HttpStatusCode.OK)
        }
        val provider = SessionCredentialProvider { null }
        val client = KtorNetworkClient(
            baseUrl = "https://api.test.com",
            credentialProvider = provider,
            baseHttpClient = HttpClient(mockEngine)
        )

        val resp = client.execute(NetworkRequest(url = "/api/protected", authPolicy = AuthPolicy.REQUIRED))
        assertEquals(401, resp.statusCode)
        assertFalse(resp.isSuccessful)
    }

    @Test
    fun test401RefreshesTokenAndRetriesOnce() = runTest {
        var attempts = 0
        var tokenUsedSecondAttempt: String? = null
        val mockEngine = MockEngine { req ->
            attempts++
            if (attempts == 1) {
                respond("{\"error\":\"unauthorized\"}", HttpStatusCode.Unauthorized)
            } else {
                tokenUsedSecondAttempt = req.headers["Authorization"]
                respond("{\"status\":\"ok\"}", HttpStatusCode.OK)
            }
        }

        val persistence = FakeCredentialPersistence()
        val store = SessionStore()
        val clear = ClearSession(persistence, store)
        persistence.save(SessionCredentials("tok_old", "ref_123"))

        var providerToken = "tok_old"
        val provider = SessionCredentialProvider { providerToken }

        val gateway = SessionRefreshGateway { _ ->
            providerToken = "tok_refreshed"
            SessionRefreshResult.Success(SessionCredentials("tok_refreshed", "ref_123"))
        }

        val coordinator = SessionRefreshCoordinator(persistence, gateway, clear, store)
        val client = KtorNetworkClient(
            baseUrl = "https://api.test.com",
            credentialProvider = provider,
            refreshCoordinator = coordinator,
            baseHttpClient = HttpClient(mockEngine)
        )

        val resp = client.execute(NetworkRequest(url = "/api/protected", authPolicy = AuthPolicy.OPTIONAL))
        assertTrue(resp.isSuccessful)
        assertEquals(2, attempts, "Must retry exactly once on 401 recovery")
        assertEquals("Bearer tok_refreshed", tokenUsedSecondAttempt)
    }
}
