package com.carbroz.partner.app.composition.graph

import com.carbroz.partner.app.composition.config.AppConfig
import com.carbroz.partner.app.composition.config.SduiEndpointConfig
import com.carbroz.partner.domain.session.credential.CredentialLoadResult
import com.carbroz.partner.domain.session.credential.CredentialPersistenceResult
import com.carbroz.partner.domain.session.credential.SessionCredentialPersistence
import com.carbroz.partner.domain.session.model.SessionCredentials
import com.carbroz.partner.domain.session.model.SessionState
import com.carbroz.partner.domain.session.refresh.SessionRefreshGateway
import com.carbroz.partner.domain.session.refresh.SessionRefreshOutcome
import com.carbroz.partner.domain.session.refresh.SessionRefreshResult
import com.carbroz.partner.engine.execution.action.ActionId
import com.carbroz.partner.engine.execution.action.ActionParameters
import com.carbroz.partner.engine.execution.action.ActionSpec
import com.carbroz.partner.engine.execution.action.ActionType
import com.carbroz.partner.engine.execution.result.ExecutionResult
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class AppGraphTest {

    private class FakeCredentialPersistence(
        private var credentials: SessionCredentials? = null
    ) : SessionCredentialPersistence {
        override suspend fun load(): CredentialLoadResult {
            val c = credentials
            return if (c != null) CredentialLoadResult.Found(c) else CredentialLoadResult.NotFound
        }

        override suspend fun save(credentials: SessionCredentials): CredentialPersistenceResult {
            this.credentials = credentials
            return CredentialPersistenceResult.Success
        }

        override suspend fun clear(): CredentialPersistenceResult {
            this.credentials = null
            return CredentialPersistenceResult.Success
        }
    }

    @Test
    fun testAppGraphInstantiatesDependenciesAndFactoryMethods() = runTest {
        val testScope = TestScope(UnconfinedTestDispatcher(testScheduler))
        val config = AppConfig(baseUrl = "https://api.carbroz.com")
        val endpointConfig = SduiEndpointConfig()
        val graph = AppGraph(
            config = config,
            credentialPersistence = FakeCredentialPersistence(),
            endpointConfig = endpointConfig
        )

        assertNotNull(graph.networkClient)
        assertNotNull(graph.sduiProcessor)
        assertNotNull(graph.router)
        assertNotNull(graph.screenRepository)

        val splashStore = graph.createSplashStore(testScope)
        assertNotNull(splashStore)

        val controller = graph.createHostController(testScope)
        assertNotNull(controller)
        assertEquals("splash", graph.router.state.value.activeEntry.destination.route)
        assertEquals(SduiEndpointConfig.ROOT_SDUI_ENDPOINT, graph.endpointConfig.entryEndpoint)
    }

    @Test
    fun testAppGraphWiresCanonicalCredentialProviderAndRestorer() = runTest {
        val persistence = FakeCredentialPersistence(
            SessionCredentials(accessToken = "token_abc", refreshToken = "refresh_xyz")
        )
        val graph = AppGraph(
            config = AppConfig(baseUrl = "https://api.carbroz.com"),
            credentialPersistence = persistence
        )

        // 1. Persisted credentials available through canonical provider
        assertEquals("token_abc", graph.credentialProvider.getAccessToken())

        // 2. Startup restoration invokes real SessionRestorer and updates sessionStore state
        assertEquals(SessionState.Unknown, graph.sessionStore.state.value)
        graph.startupOrchestrator.initialize()
        assertEquals(SessionState.Authenticated, graph.sessionStore.state.value)
    }

    @Test
    fun testAppGraphWiresLogoutClearingCanonicalCredentials() = runTest {
        val persistence = FakeCredentialPersistence(
            SessionCredentials(accessToken = "token_abc", refreshToken = "refresh_xyz")
        )
        val graph = AppGraph(
            config = AppConfig(baseUrl = "https://api.carbroz.com"),
            credentialPersistence = persistence
        )

        graph.sessionRestorer.restore()
        assertEquals(SessionState.Authenticated, graph.sessionStore.state.value)

        val logoutAction = ActionSpec.create(
            id = ActionId("act_logout"),
            type = ActionType.AUTH_LOGOUT,
            parameters = ActionParameters.EMPTY
        )
        val result = graph.actionDispatcher.dispatch(logoutAction)

        assertTrue(result is ExecutionResult.Success)
        assertEquals(SessionState.Unauthenticated, graph.sessionStore.state.value)
        assertNull(graph.credentialProvider.getAccessToken())
    }

    @Test
    fun testAppGraphRefreshCoordinatorSharesCanonicalSessionStore() = runTest {
        val persistence = FakeCredentialPersistence(
            SessionCredentials(accessToken = "token_old", refreshToken = "refresh_old")
        )
        val refreshGateway = SessionRefreshGateway {
            SessionRefreshResult.Success(SessionCredentials("token_new", "refresh_new"))
        }
        val graph = AppGraph(
            config = AppConfig(baseUrl = "https://api.carbroz.com"),
            credentialPersistence = persistence,
            refreshGateway = refreshGateway
        )

        assertEquals(SessionState.Unknown, graph.sessionStore.state.value)

        val coordinator = graph.refreshCoordinator
        assertNotNull(coordinator)
        val outcome = coordinator.refresh("token_old")

        assertEquals(SessionRefreshOutcome.Refreshed, outcome)
        assertEquals(SessionState.Authenticated, graph.sessionStore.state.value)
    }
}
