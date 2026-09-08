package com.carbroz.runtime.application.startup

import com.carbroz.foundation.session.SessionProvider
import com.carbroz.foundation.session.SessionState
import com.carbroz.runtime.application.bootstrap.BootstrapMaintenance
import com.carbroz.runtime.application.bootstrap.BootstrapRepository
import com.carbroz.runtime.application.bootstrap.BootstrapRepositoryFailure
import com.carbroz.runtime.application.bootstrap.BootstrapRepositoryResult
import com.carbroz.runtime.application.bootstrap.BootstrapSnapshot
import com.carbroz.runtime.application.bootstrap.BootstrapUpdate
import com.carbroz.runtime.application.bootstrap.ResolveBootstrapUseCase
import com.carbroz.runtime.application.bootstrap.StartupPayloadDecodeResult
import com.carbroz.runtime.application.bootstrap.StartupPayloadDecoder
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class BootstrapStartupTaskTest {
    @Test
    fun `task adapts resolved use case outcome`() = runTest {
        val task = task(BootstrapRepositoryResult.Success(snapshot()))

        val result = assertIs<StartupTaskResult.Resolved>(task.execute())
        assertIs<StartupResolution.Ready>(result.resolution)
    }

    @Test
    fun `task adapts failed use case outcome without adding policy`() = runTest {
        val task = task(BootstrapRepositoryResult.Failure(BootstrapRepositoryFailure.Http(401)))

        assertEquals(
            StartupTaskResult.Failure(
                StartupFailure.Expected("bootstrap_http_401", recoverable = true),
            ),
            task.execute(),
        )
    }

    private fun task(repositoryResult: BootstrapRepositoryResult): BootstrapStartupTask {
        val useCase = ResolveBootstrapUseCase(
            repository = BootstrapRepository { repositoryResult },
            sessionProvider = SessionProvider { SessionState.SignedOut },
            payloadDecoder = StartupPayloadDecoder { StartupPayloadDecodeResult.Success(TestPayload) },
        )
        return BootstrapStartupTask(useCase)
    }

    private fun snapshot() = BootstrapSnapshot(
        authenticated = false,
        maintenance = BootstrapMaintenance(enabled = false),
        update = BootstrapUpdate(
            required = false,
            optional = false,
            minimumVersion = "1.0.0",
            latestVersion = "1.0.0",
        ),
        nextPayload = "{\"screen\":\"test\"}",
    )

    private data object TestPayload : StartupPayload
}
