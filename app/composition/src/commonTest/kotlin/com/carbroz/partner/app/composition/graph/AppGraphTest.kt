package com.carbroz.partner.app.composition.graph

import com.carbroz.partner.app.composition.config.AppConfig
import com.carbroz.partner.app.composition.config.SduiEndpointConfig
import com.carbroz.partner.domain.session.credential.SessionCredentialPersistence
import com.carbroz.partner.domain.session.model.CredentialLoadResult
import com.carbroz.partner.domain.session.model.SessionCredentials
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

@OptIn(ExperimentalCoroutinesApi::class)
class AppGraphTest {

    private class FakeCredentialPersistence : SessionCredentialPersistence {
        override suspend fun load(): CredentialLoadResult = CredentialLoadResult.NotFound
        override suspend fun save(credentials: SessionCredentials): Boolean = true
        override suspend fun clear(): Boolean = true
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
        assertEquals("splash", graph.router.currentState.activeEntry.destination.route)
        assertEquals(SduiEndpointConfig.ROOT_SDUI_ENDPOINT, graph.endpointConfig.entryEndpoint)
    }
}
