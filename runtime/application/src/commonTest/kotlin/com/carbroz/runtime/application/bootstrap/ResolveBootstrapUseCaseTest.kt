package com.carbroz.runtime.application.bootstrap

import com.carbroz.foundation.security.Secret
import com.carbroz.foundation.session.AuthTokens
import com.carbroz.foundation.session.SessionProvider
import com.carbroz.foundation.session.SessionState
import com.carbroz.foundation.session.SessionSubject
import com.carbroz.runtime.application.startup.StartupBlocker
import com.carbroz.runtime.application.startup.StartupFailure
import com.carbroz.runtime.application.startup.StartupNotice
import com.carbroz.runtime.application.startup.StartupPayload
import com.carbroz.runtime.application.startup.StartupResolution
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class ResolveBootstrapUseCaseTest {
    @Test
    fun `guest bootstrap resolves Ready for signed out canonical session`() = runTest {
        val result = resolve(snapshot(authenticated = false), SessionState.SignedOut)
        val resolution = assertIs<StartupResolution.Ready>(assertIs<ResolveBootstrapResult.Resolved>(result).resolution)
        assertEquals(TestPayload, resolution.payload)
    }

    @Test
    fun `authenticated bootstrap resolves Ready for authenticated canonical session`() = runTest {
        val result = resolve(snapshot(authenticated = true), authenticatedSession())
        assertIs<StartupResolution.Ready>(assertIs<ResolveBootstrapResult.Resolved>(result).resolution)
    }

    @Test
    fun `backend and local authentication mismatch fails closed`() = runTest {
        val result = assertIs<ResolveBootstrapResult.Failed>(
            resolve(snapshot(authenticated = true), SessionState.SignedOut),
        )

        assertEquals(
            StartupFailure.Expected("bootstrap_session_mismatch", recoverable = false),
            result.failure,
        )
    }

    @Test
    fun `required update has precedence over maintenance`() = runTest {
        val result = resolve(
            snapshot(
                maintenance = true,
                required = true,
                updateUri = "https://example.com/update",
            ),
            SessionState.SignedOut,
        )

        val blocker = assertIs<StartupBlocker.RequiredUpdate>(
            assertIs<StartupResolution.Blocked>(assertIs<ResolveBootstrapResult.Resolved>(result).resolution).blocker,
        )
        assertEquals("https://example.com/update", blocker.updateUri)
    }

    @Test
    fun `required update without URI fails closed`() = runTest {
        val result = assertIs<ResolveBootstrapResult.Failed>(
            resolve(snapshot(required = true, updateUri = null), SessionState.SignedOut),
        )

        assertEquals(
            StartupFailure.Expected("bootstrap_required_update_missing_store_url", recoverable = false),
            result.failure,
        )
    }

    @Test
    fun `maintenance is retryable blocked outcome not failure`() = runTest {
        val result = resolve(snapshot(maintenance = true), SessionState.SignedOut)
        val blocker = assertIs<StartupBlocker.Maintenance>(
            assertIs<StartupResolution.Blocked>(assertIs<ResolveBootstrapResult.Resolved>(result).resolution).blocker,
        )
        assertEquals(true, blocker.retryable)
    }

    @Test
    fun `optional update remains Ready and adds notice`() = runTest {
        val result = resolve(
            snapshot(optional = true, latestVersion = "2.0.0", updateUri = "https://example.com/update"),
            SessionState.SignedOut,
        )

        val resolution = assertIs<StartupResolution.Ready>(assertIs<ResolveBootstrapResult.Resolved>(result).resolution)
        assertEquals(
            listOf(StartupNotice.OptionalUpdate("2.0.0", "https://example.com/update")),
            resolution.notices,
        )
    }

    @Test
    fun `invalid startup payload fails closed with stable bootstrap code`() = runTest {
        val useCase = useCase(
            repositoryResult = BootstrapRepositoryResult.Success(snapshot()),
            sessionState = SessionState.SignedOut,
            decoder = StartupPayloadDecoder { StartupPayloadDecodeResult.Failure("dynamic_instruction_invalid_endpoint") },
        )

        val result = assertIs<ResolveBootstrapResult.Failed>(useCase())
        assertEquals(
            StartupFailure.Expected("bootstrap_dynamic_instruction_invalid_endpoint", recoverable = false),
            result.failure,
        )
    }

    @Test
    fun `repository failure recovery semantics are explicit`() = runTest {
        val cases = listOf(
            BootstrapRepositoryFailure.Offline to StartupFailure.Expected("bootstrap_offline", true),
            BootstrapRepositoryFailure.Timeout to StartupFailure.Expected("bootstrap_timeout", true),
            BootstrapRepositoryFailure.Transport to StartupFailure.Expected("bootstrap_transport", true),
            BootstrapRepositoryFailure.Http(401) to StartupFailure.Expected("bootstrap_http_401", true),
            BootstrapRepositoryFailure.Http(408) to StartupFailure.Expected("bootstrap_http_408", true),
            BootstrapRepositoryFailure.Http(429) to StartupFailure.Expected("bootstrap_http_429", true),
            BootstrapRepositoryFailure.Http(503) to StartupFailure.Expected("bootstrap_http_503", true),
            BootstrapRepositoryFailure.Http(400) to StartupFailure.Expected("bootstrap_http_400", false),
            BootstrapRepositoryFailure.InvalidPayload("bootstrap_invalid_payload") to
                StartupFailure.Expected("bootstrap_invalid_payload", false),
        )

        cases.forEach { (repositoryFailure, expected) ->
            val useCase = useCase(
                repositoryResult = BootstrapRepositoryResult.Failure(repositoryFailure),
                sessionState = SessionState.SignedOut,
            )
            assertEquals(expected, assertIs<ResolveBootstrapResult.Failed>(useCase()).failure)
        }
    }

    private suspend fun resolve(snapshot: BootstrapSnapshot, sessionState: SessionState): ResolveBootstrapResult =
        useCase(
            repositoryResult = BootstrapRepositoryResult.Success(snapshot),
            sessionState = sessionState,
        )()

    private fun useCase(
        repositoryResult: BootstrapRepositoryResult,
        sessionState: SessionState,
        decoder: StartupPayloadDecoder = StartupPayloadDecoder { StartupPayloadDecodeResult.Success(TestPayload) },
    ) = ResolveBootstrapUseCase(
        repository = BootstrapRepository { repositoryResult },
        sessionProvider = SessionProvider { sessionState },
        payloadDecoder = decoder,
    )

    private fun snapshot(
        authenticated: Boolean = false,
        maintenance: Boolean = false,
        required: Boolean = false,
        optional: Boolean = false,
        latestVersion: String = "1.0.0",
        updateUri: String? = null,
    ) = BootstrapSnapshot(
        authenticated = authenticated,
        maintenance = BootstrapMaintenance(
            enabled = maintenance,
            title = "Maintenance",
            message = "Try later",
        ),
        update = BootstrapUpdate(
            required = required,
            optional = optional,
            minimumVersion = "1.0.0",
            latestVersion = latestVersion,
            updateUri = updateUri,
        ),
        nextPayload = "{\"screen\":\"test\"}",
    )

    private fun authenticatedSession(): SessionState.Authenticated = SessionState.Authenticated(
        subject = SessionSubject("partner-1"),
        tokens = AuthTokens(
            accessToken = Secret.of("access"),
            refreshToken = Secret.of("refresh"),
            accessTokenExpiresAtEpochMilliseconds = 123_456L,
        ),
    )

    private data object TestPayload : StartupPayload
}
