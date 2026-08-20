package com.carbroz.partner.feature.splash

import com.carbroz.partner.core.observability.logger.BoundLogger
import com.carbroz.partner.core.observability.logger.StructuredLogger
import com.carbroz.partner.core.observability.model.LogAttribute
import com.carbroz.partner.core.observability.model.LogCategory
import com.carbroz.partner.core.observability.model.LogLevel
import com.carbroz.partner.core.observability.model.TraceContext
import com.carbroz.partner.domain.session.credential.SessionCredentialPersistence
import com.carbroz.partner.domain.session.model.CredentialLoadResult
import com.carbroz.partner.domain.session.model.SessionCredentials
import com.carbroz.partner.domain.session.restore.SessionRestorer
import com.carbroz.partner.domain.session.store.SessionStore
import com.carbroz.partner.feature.splash.orchestrator.ImmediateStartupOrchestrator
import com.carbroz.partner.feature.splash.orchestrator.StartupDestination
import com.carbroz.partner.feature.splash.orchestrator.StartupOrchestrator
import com.carbroz.partner.feature.splash.orchestrator.StartupResult
import com.carbroz.partner.feature.splash.store.SplashEffect
import com.carbroz.partner.feature.splash.store.SplashIntent
import com.carbroz.partner.feature.splash.store.SplashState
import com.carbroz.partner.feature.splash.store.SplashStore
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class SplashStoreTest {

    private class NoOpLogger : StructuredLogger {
        override fun isLevelEnabled(level: LogLevel): Boolean = false
        override fun withSource(sourceClass: String, defaultTraceContext: TraceContext?): BoundLogger {
            return object : BoundLogger {
                override fun log(
                    level: LogLevel,
                    category: LogCategory,
                    sourceFunction: String,
                    event: String,
                    message: String,
                    attributes: Map<String, LogAttribute>,
                    traceContext: TraceContext?,
                    durationMs: Long?,
                    throwable: Throwable?
                ) {}
            }
        }
    }

    private class FakeCredentialPersistence : SessionCredentialPersistence {
        override suspend fun load(): CredentialLoadResult = CredentialLoadResult.NotFound
        override suspend fun save(credentials: SessionCredentials): Boolean = true
        override suspend fun clear(): Boolean = true
    }

    @Test
    fun testInitializationSuccessEmitsNavigateEffect() = runTest {
        val testScope = TestScope(UnconfinedTestDispatcher(testScheduler))
        val persistence = FakeCredentialPersistence()
        val sessionStore = SessionStore()
        val restorer = SessionRestorer(persistence, sessionStore)
        val orchestrator = ImmediateStartupOrchestrator(restorer)
        val store = SplashStore(
            scope = testScope,
            logger = NoOpLogger(),
            orchestrator = orchestrator
        )

        val effects = mutableListOf<SplashEffect>()
        val job = testScope.launch {
            store.effects.toList(effects)
        }

        store.dispatch(SplashIntent.Initialize)

        assertTrue(store.state.value is SplashState.Success)
        assertEquals(StartupDestination.ServerDrivenUi, (store.state.value as SplashState.Success).destination)
        assertEquals(1, effects.size)
        assertEquals(SplashEffect.NavigateToDestination(StartupDestination.ServerDrivenUi), effects[0])

        job.cancel()
    }

    @Test
    fun testInitializationFailureEmitsErrorState() = runTest {
        val testScope = TestScope(UnconfinedTestDispatcher(testScheduler))
        val failingOrchestrator = object : StartupOrchestrator {
            override suspend fun initialize(): StartupResult {
                return StartupResult.Failure("Network failure")
            }
        }

        val store = SplashStore(
            scope = testScope,
            logger = NoOpLogger(),
            orchestrator = failingOrchestrator
        )

        store.dispatch(SplashIntent.Initialize)

        assertTrue(store.state.value is SplashState.Error)
        assertEquals("Network failure", (store.state.value as SplashState.Error).message)
    }
}
