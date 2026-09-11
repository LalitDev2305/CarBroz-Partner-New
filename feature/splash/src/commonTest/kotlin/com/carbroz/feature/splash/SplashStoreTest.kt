package com.carbroz.feature.splash

import com.carbroz.foundation.lifecycle.AppLifecycleState
import com.carbroz.foundation.session.SessionPersistence
import com.carbroz.foundation.session.SessionPersistenceResult
import com.carbroz.foundation.session.SessionRestoreResult
import com.carbroz.foundation.session.SessionState
import com.carbroz.foundation.session.SessionStore
import com.carbroz.runtime.application.startup.BootstrapMaintenance
import com.carbroz.runtime.application.startup.BootstrapRepository
import com.carbroz.runtime.application.startup.BootstrapRepositoryResult
import com.carbroz.runtime.application.startup.BootstrapSnapshot
import com.carbroz.runtime.application.startup.BootstrapUpdate
import com.carbroz.runtime.application.startup.PartnerConfig
import com.carbroz.runtime.application.startup.PartnerConfigStore
import com.carbroz.runtime.application.startup.PartnerFeatures
import com.carbroz.runtime.application.startup.ResolveStartupUseCase
import com.carbroz.runtime.application.startup.StartupAuthentication
import com.carbroz.runtime.application.startup.StartupDestination
import com.carbroz.runtime.application.startup.StartupRequestMethod
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class SplashStoreTest {
    @Test
    fun `foreground starts startup once and emits one typed navigation effect`() = runTest {
        val repository = CountingRepository(successSnapshot())
        val store = SplashStore(useCase(repository), backgroundScope)
        val effects = mutableListOf<SplashEffect>()
        val collector = backgroundScope.launch { store.effects.collect(effects::add) }

        store.dispatch(SplashIntent.LifecycleChanged(AppLifecycleState.Foreground))
        runCurrent()
        store.dispatch(SplashIntent.LifecycleChanged(AppLifecycleState.Foreground))
        runCurrent()

        assertEquals(1, repository.calls)
        assertEquals(SplashState.Ready, store.state.value)
        assertEquals(
            listOf(SplashEffect.Navigate(successSnapshot().nextScreen)),
            effects,
        )
        collector.cancel()
        store.close()
    }

    @Test
    fun `background cancels active startup work`() = runTest {
        var started = false
        var cancelled = false
        val repository = BootstrapRepository {
            started = true
            try {
                awaitCancellation()
            } catch (cancellation: CancellationException) {
                cancelled = true
                throw cancellation
            }
        }
        val store = SplashStore(useCase(repository), backgroundScope)

        store.dispatch(SplashIntent.LifecycleChanged(AppLifecycleState.Foreground))
        runCurrent()
        assertTrue(started)

        store.dispatch(SplashIntent.LifecycleChanged(AppLifecycleState.Background))
        runCurrent()

        assertTrue(cancelled)
        store.close()
    }

    @Test
    fun `required update maps to state and emits update effect`() = runTest {
        val snapshot = successSnapshot(requiredUpdate = true, updateUri = "https://example.com/update")
        val store = SplashStore(useCase(CountingRepository(snapshot)), backgroundScope)
        val effects = mutableListOf<SplashEffect>()
        val collector = backgroundScope.launch { store.effects.collect(effects::add) }

        store.dispatch(SplashIntent.LifecycleChanged(AppLifecycleState.Foreground))
        runCurrent()
        store.dispatch(SplashIntent.UpdateClicked)
        runCurrent()

        assertIs<SplashState.RequiredUpdate>(store.state.value)
        assertEquals(listOf(SplashEffect.OpenUpdateUri("https://example.com/update")), effects)
        collector.cancel()
        store.close()
    }

    @Test
    fun `maintenance retry performs a fresh startup attempt`() = runTest {
        val repository = SequencedRepository(
            listOf(
                successSnapshot(maintenanceEnabled = true),
                successSnapshot(),
            ),
        )
        val store = SplashStore(useCase(repository), backgroundScope)

        store.dispatch(SplashIntent.LifecycleChanged(AppLifecycleState.Foreground))
        runCurrent()
        assertIs<SplashState.Maintenance>(store.state.value)

        store.dispatch(SplashIntent.RetryClicked)
        runCurrent()

        assertEquals(2, repository.calls)
        assertEquals(SplashState.Ready, store.state.value)
        store.close()
    }

    private fun useCase(repository: BootstrapRepository): ResolveStartupUseCase = ResolveStartupUseCase(
        sessionStore = SessionStore(FakeSessionPersistence()),
        bootstrapRepository = repository,
        partnerConfigStore = PartnerConfigStore(),
    )

    private fun successSnapshot(
        maintenanceEnabled: Boolean = false,
        requiredUpdate: Boolean = false,
        updateUri: String? = null,
    ): BootstrapSnapshot = BootstrapSnapshot(
        authenticated = false,
        maintenance = BootstrapMaintenance(
            enabled = maintenanceEnabled,
            title = "Maintenance",
            message = "Try later",
        ),
        config = PartnerConfig(
            version = "1",
            features = PartnerFeatures(true, true, true),
            update = BootstrapUpdate(
                required = requiredUpdate,
                optional = false,
                minimumVersion = "1.0.0",
                latestVersion = "1.0.0",
                updateUri = updateUri,
            ),
        ),
        nextScreen = StartupDestination(
            screenId = "partner_login",
            templateId = "tpl_7K2M9Q",
            templateType = "form_template",
            endpoint = "/api/v1/partner/screen/auth_login",
            method = StartupRequestMethod.GET,
            authentication = StartupAuthentication.NONE,
        ),
    )

    private class CountingRepository(
        private val snapshot: BootstrapSnapshot,
    ) : BootstrapRepository {
        var calls: Int = 0
        override suspend fun load(): BootstrapRepositoryResult {
            calls += 1
            return BootstrapRepositoryResult.Success(snapshot)
        }
    }

    private class SequencedRepository(
        private val snapshots: List<BootstrapSnapshot>,
    ) : BootstrapRepository {
        var calls: Int = 0
        override suspend fun load(): BootstrapRepositoryResult {
            val snapshot = snapshots[calls.coerceAtMost(snapshots.lastIndex)]
            calls += 1
            return BootstrapRepositoryResult.Success(snapshot)
        }
    }

    private class FakeSessionPersistence : SessionPersistence {
        override suspend fun restore(): SessionRestoreResult = SessionRestoreResult.NoSession
        override suspend fun save(session: SessionState.Authenticated): SessionPersistenceResult = SessionPersistenceResult.Success
        override suspend fun clear(): SessionPersistenceResult = SessionPersistenceResult.Success
    }
}
