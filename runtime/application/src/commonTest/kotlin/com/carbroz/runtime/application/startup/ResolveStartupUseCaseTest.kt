package com.carbroz.runtime.application.startup

import com.carbroz.foundation.session.SessionPersistence
import com.carbroz.foundation.session.SessionPersistenceResult
import com.carbroz.foundation.session.SessionRestoreResult
import com.carbroz.foundation.session.SessionState
import com.carbroz.foundation.session.SessionStore
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull

class ResolveStartupUseCaseTest {
    @Test
    fun `signed out startup restores session loads bootstrap stores config and returns typed destination`() = runTest {
        val configStore = PartnerConfigStore()
        val repository = FakeBootstrapRepository(successSnapshot())
        val useCase = ResolveStartupUseCase(
            sessionStore = SessionStore(FakeSessionPersistence()),
            bootstrapRepository = repository,
            partnerConfigStore = configStore,
        )

        val result = assertIs<StartupResult.Ready>(useCase())

        assertEquals("partner_login", result.destination.screenId)
        assertEquals("tpl_7K2M9Q", result.destination.templateId)
        assertEquals("form_template", result.destination.templateType)
        assertEquals(1, repository.calls)
        assertEquals("1", configStore.current()?.version)
        assertEquals(true, configStore.current()?.features?.registrationEnabled)
    }

    @Test
    fun `required update has precedence over maintenance`() = runTest {
        val snapshot = successSnapshot(
            maintenanceEnabled = true,
            requiredUpdate = true,
            updateUri = "https://store.example/app",
        )
        val result = useCase(snapshot)()

        val required = assertIs<StartupResult.RequiredUpdate>(result)
        assertEquals("https://store.example/app", required.updateUri)
    }

    @Test
    fun `required update without store uri fails closed`() = runTest {
        val result = useCase(
            successSnapshot(requiredUpdate = true, updateUri = null),
        )()

        val failure = assertIs<StartupResult.Failure>(result)
        assertEquals("bootstrap_required_update_missing_store_url", failure.code)
        assertEquals(false, failure.recoverable)
    }

    @Test
    fun `maintenance is a blocking application result`() = runTest {
        val result = useCase(successSnapshot(maintenanceEnabled = true))()

        val maintenance = assertIs<StartupResult.Maintenance>(result)
        assertEquals("Maintenance", maintenance.title)
        assertEquals(true, maintenance.retryable)
    }

    @Test
    fun `repository failure maps to stable startup failure`() = runTest {
        val useCase = ResolveStartupUseCase(
            sessionStore = SessionStore(FakeSessionPersistence()),
            bootstrapRepository = BootstrapRepository {
                BootstrapRepositoryResult.Failure(BootstrapRepositoryFailure.Timeout)
            },
            partnerConfigStore = PartnerConfigStore(),
        )

        val failure = assertIs<StartupResult.Failure>(useCase())
        assertEquals("bootstrap_timeout", failure.code)
        assertEquals(true, failure.recoverable)
    }

    @Test
    fun `session restore failure stops before bootstrap`() = runTest {
        val repository = FakeBootstrapRepository(successSnapshot())
        val sessionStore = SessionStore(
            FakeSessionPersistence(
                restoreResult = SessionRestoreResult.Rejected(
                    com.carbroz.foundation.session.SessionRestoreFailure.StorageUnavailable,
                ),
            ),
        )
        val useCase = ResolveStartupUseCase(sessionStore, repository, PartnerConfigStore())

        val failure = assertIs<StartupResult.Failure>(useCase())
        assertEquals("session_restore_failed", failure.code)
        assertEquals(0, repository.calls)
    }

    @Test
    fun `config store starts empty and keeps optional update metadata`() = runTest {
        val configStore = PartnerConfigStore()
        assertNull(configStore.current())
        val useCase = ResolveStartupUseCase(
            SessionStore(FakeSessionPersistence()),
            FakeBootstrapRepository(successSnapshot(optionalUpdate = true)),
            configStore,
        )

        assertIs<StartupResult.Ready>(useCase())
        assertEquals(true, configStore.current()?.update?.optional)
    }

    private fun useCase(snapshot: BootstrapSnapshot): ResolveStartupUseCase = ResolveStartupUseCase(
        sessionStore = SessionStore(FakeSessionPersistence()),
        bootstrapRepository = FakeBootstrapRepository(snapshot),
        partnerConfigStore = PartnerConfigStore(),
    )

    private fun successSnapshot(
        maintenanceEnabled: Boolean = false,
        requiredUpdate: Boolean = false,
        optionalUpdate: Boolean = false,
        updateUri: String? = null,
    ): BootstrapSnapshot {
        val update = BootstrapUpdate(
            required = requiredUpdate,
            optional = optionalUpdate,
            minimumVersion = "1.0.0",
            latestVersion = if (optionalUpdate) "1.1.0" else "1.0.0",
            updateUri = updateUri,
        )
        return BootstrapSnapshot(
            authenticated = false,
            maintenance = BootstrapMaintenance(
                enabled = maintenanceEnabled,
                title = "Maintenance",
                message = "Try later",
            ),
            config = PartnerConfig(
                version = "1",
                features = PartnerFeatures(
                    registrationEnabled = true,
                    individualPartnerEnabled = true,
                    organizationPartnerEnabled = true,
                ),
                update = update,
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
    }

    private class FakeBootstrapRepository(
        private val snapshot: BootstrapSnapshot,
    ) : BootstrapRepository {
        var calls: Int = 0
        override suspend fun load(): BootstrapRepositoryResult {
            calls += 1
            return BootstrapRepositoryResult.Success(snapshot)
        }
    }

    private class FakeSessionPersistence(
        private val restoreResult: SessionRestoreResult = SessionRestoreResult.NoSession,
    ) : SessionPersistence {
        override suspend fun restore(): SessionRestoreResult = restoreResult
        override suspend fun save(session: SessionState.Authenticated): SessionPersistenceResult =
            SessionPersistenceResult.Success
        override suspend fun clear(): SessionPersistenceResult = SessionPersistenceResult.Success
    }
}
