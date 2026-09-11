package com.carbroz.data.bootstrap

import com.carbroz.data.network.NetworkAuthentication
import com.carbroz.data.network.NetworkDataSource
import com.carbroz.data.network.NetworkFailure
import com.carbroz.data.network.NetworkMethod
import com.carbroz.data.network.NetworkRequest
import com.carbroz.data.network.NetworkResponse
import com.carbroz.data.network.NetworkResult
import com.carbroz.runtime.application.startup.BootstrapRepositoryFailure
import com.carbroz.runtime.application.startup.BootstrapRepositoryResult
import com.carbroz.runtime.application.startup.StartupAuthentication
import com.carbroz.runtime.application.startup.StartupRequestMethod
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

class RemoteBootstrapRepositoryTest {
    @Test
    fun `repository uses canonical GET config route optional session and no manual headers`() = runTest {
        var captured: NetworkRequest? = null
        val repository = RemoteBootstrapRepository(
            NetworkDataSource { request ->
                captured = request
                successResponse(authenticated = false)
            },
        )

        assertIs<BootstrapRepositoryResult.Success>(repository.load())
        assertEquals(NetworkMethod.GET, captured?.method)
        assertEquals("/api/v1/partner/config/bootstrap", captured?.endpoint?.value)
        assertEquals(NetworkAuthentication.OPTIONAL_SESSION, captured?.authentication)
        assertTrue(captured?.headers?.isEmpty() == true)
    }

    @Test
    fun `valid guest envelope maps typed destination and reusable config`() = runTest {
        val result = assertIs<BootstrapRepositoryResult.Success>(
            repository(successResponse(authenticated = false)).load(),
        )

        val snapshot = result.snapshot
        assertEquals(false, snapshot.authenticated)
        assertEquals(false, snapshot.maintenance.enabled)
        assertEquals(false, snapshot.config.update.required)
        assertEquals(false, snapshot.config.update.optional)
        assertNull(snapshot.config.update.updateUri)
        assertEquals("1", snapshot.config.version)
        assertTrue(snapshot.config.features.registrationEnabled)
        assertEquals(false, snapshot.config.features.organizationPartnerEnabled)
        assertEquals("partner_login", snapshot.nextScreen.screenId)
        assertEquals("tpl_7K2M9Q", snapshot.nextScreen.templateId)
        assertEquals("form_template", snapshot.nextScreen.templateType)
        assertEquals("/api/v1/partner/screen/auth_login", snapshot.nextScreen.endpoint)
        assertEquals(StartupRequestMethod.GET, snapshot.nextScreen.method)
        assertEquals(StartupAuthentication.NONE, snapshot.nextScreen.authentication)
    }

    @Test
    fun `canonical network failures map without transport leakage`() = runTest {
        val cases = listOf(
            NetworkFailure.Offline to BootstrapRepositoryFailure.Offline,
            NetworkFailure.Timeout to BootstrapRepositoryFailure.Timeout,
            NetworkFailure.Transport to BootstrapRepositoryFailure.Transport,
            NetworkFailure.Http(503) to BootstrapRepositoryFailure.Http(503),
            NetworkFailure.InvalidRequest("bad") to BootstrapRepositoryFailure.InvalidPayload("bootstrap_invalid_request"),
        )

        cases.forEach { (networkFailure, expected) ->
            val result = assertIs<BootstrapRepositoryResult.Failure>(
                repository(NetworkResult.Failure(networkFailure)).load(),
            )
            assertEquals(expected, result.reason)
        }
    }

    @Test
    fun `invalid canonical envelope fails closed`() = runTest {
        val body = buildJsonObject {
            put("status", 200)
            put("code", "REJECTED")
            put("message", "rejected")
        }
        val result = assertIs<BootstrapRepositoryResult.Failure>(
            repository(NetworkResult.Success(NetworkResponse(200, body = body))).load(),
        )
        assertEquals(
            BootstrapRepositoryFailure.InvalidPayload("bootstrap_unsuccessful_envelope"),
            result.reason,
        )
    }

    @Test
    fun `invalid startup method or authentication fails closed`() = runTest {
        val invalidMethod = assertIs<BootstrapRepositoryResult.Failure>(
            repository(successResponse(authenticated = false, method = "POST")).load(),
        )
        assertEquals(
            BootstrapRepositoryFailure.InvalidPayload("bootstrap_invalid_next_screen"),
            invalidMethod.reason,
        )

        val invalidAuth = assertIs<BootstrapRepositoryResult.Failure>(
            repository(successResponse(authenticated = false, authentication = "OPTIONAL_SESSION")).load(),
        )
        assertEquals(
            BootstrapRepositoryFailure.InvalidPayload("bootstrap_invalid_next_screen"),
            invalidAuth.reason,
        )
    }

    @Test
    fun `malformed or missing body fails closed`() = runTest {
        val malformed = assertIs<BootstrapRepositoryResult.Failure>(
            repository(NetworkResult.Success(NetworkResponse(200, body = JsonPrimitive("bad")))).load(),
        )
        assertEquals(
            BootstrapRepositoryFailure.InvalidPayload("bootstrap_invalid_payload"),
            malformed.reason,
        )

        val missing = assertIs<BootstrapRepositoryResult.Failure>(
            repository(NetworkResult.Success(NetworkResponse(200, body = null))).load(),
        )
        assertEquals(
            BootstrapRepositoryFailure.InvalidPayload("bootstrap_missing_body"),
            missing.reason,
        )
    }

    private fun repository(result: NetworkResult): RemoteBootstrapRepository =
        RemoteBootstrapRepository(NetworkDataSource { result })

    private fun successResponse(
        authenticated: Boolean,
        configVersion: String = "1",
        minimumVersion: String = "1.0.0",
        latestVersion: String = "1.0.0",
        required: Boolean = false,
        optional: Boolean = false,
        method: String = "GET",
        authentication: String = "NONE",
    ): NetworkResult.Success = NetworkResult.Success(
        NetworkResponse(
            statusCode = 200,
            body = buildJsonObject {
                put("status", 200)
                put("code", "SUCCESS")
                put("message", "Partner bootstrap completed")
                put("data", buildJsonObject {
                    put("config", buildJsonObject {
                        put("version", configVersion)
                        put("maintenance", buildJsonObject {
                            put("enabled", false)
                            put("title", JsonNull)
                            put("message", JsonNull)
                        })
                        put("update", buildJsonObject {
                            put("required", required)
                            put("optional", optional)
                            put("minimumVersion", minimumVersion)
                            put("latestVersion", latestVersion)
                            put("storeUrl", JsonNull)
                        })
                        put("features", buildJsonObject {
                            put("registrationEnabled", true)
                            put("individualPartnerEnabled", true)
                            put("organizationPartnerEnabled", false)
                        })
                    })
                    put("startup", buildJsonObject {
                        put("authenticated", authenticated)
                        put("nextScreen", buildJsonObject {
                            put("screenId", "partner_login")
                            put("templateId", "tpl_7K2M9Q")
                            put("templateType", "form_template")
                            put("endpoint", "/api/v1/partner/screen/auth_login")
                            put("method", method)
                            put("authentication", authentication)
                        })
                    })
                })
                put("traceId", "req-test")
            },
        ),
    )
}
