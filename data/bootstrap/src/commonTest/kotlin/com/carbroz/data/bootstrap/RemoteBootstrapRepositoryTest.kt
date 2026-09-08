package com.carbroz.data.bootstrap

import com.carbroz.data.network.NetworkAuthentication
import com.carbroz.data.network.NetworkDataSource
import com.carbroz.data.network.NetworkFailure
import com.carbroz.data.network.NetworkMethod
import com.carbroz.data.network.NetworkRequest
import com.carbroz.data.network.NetworkResponse
import com.carbroz.data.network.NetworkResult
import com.carbroz.runtime.application.bootstrap.BootstrapRepositoryFailure
import com.carbroz.runtime.application.bootstrap.BootstrapRepositoryResult
import kotlinx.coroutines.test.runTest
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
    fun `repository uses exact GET route optional session and no manual headers`() = runTest {
        var captured: NetworkRequest? = null
        val repository = RemoteBootstrapRepository(
            NetworkDataSource { request ->
                captured = request
                successResponse(authenticated = false)
            },
        )

        assertIs<BootstrapRepositoryResult.Success>(repository.load())
        assertEquals(NetworkMethod.GET, captured?.method)
        assertEquals("/api/v1/partner/bootstrap", captured?.endpoint?.value)
        assertEquals(NetworkAuthentication.OPTIONAL_SESSION, captured?.authentication)
        assertTrue(captured?.headers?.isEmpty() == true)
    }

    @Test
    fun `valid guest envelope maps to normalized snapshot without feature flags`() = runTest {
        val result = assertIs<BootstrapRepositoryResult.Success>(
            repository(successResponse(authenticated = false)).load(),
        )

        val snapshot = result.snapshot
        assertEquals(false, snapshot.authenticated)
        assertEquals(false, snapshot.maintenance.enabled)
        assertEquals(false, snapshot.update.required)
        assertEquals(false, snapshot.update.optional)
        assertNull(snapshot.update.updateUri)
        assertTrue(snapshot.nextPayload.contains("partner_login"))
    }

    @Test
    fun `valid authenticated envelope preserves authenticated semantic`() = runTest {
        val result = assertIs<BootstrapRepositoryResult.Success>(
            repository(successResponse(authenticated = true)).load(),
        )

        assertEquals(true, result.snapshot.authenticated)
    }

    @Test
    fun `malformed typed body fails closed`() = runTest {
        val result = assertIs<BootstrapRepositoryResult.Failure>(
            repository(NetworkResult.Success(NetworkResponse(200, body = JsonPrimitive("bad")))).load(),
        )

        assertEquals(
            BootstrapRepositoryFailure.InvalidPayload("bootstrap_invalid_payload"),
            result.reason,
        )
    }

    @Test
    fun `missing body fails closed`() = runTest {
        val result = assertIs<BootstrapRepositoryResult.Failure>(
            repository(NetworkResult.Success(NetworkResponse(200, body = null))).load(),
        )

        assertEquals(
            BootstrapRepositoryFailure.InvalidPayload("bootstrap_missing_body"),
            result.reason,
        )
    }

    @Test
    fun `unsuccessful envelope fails closed`() = runTest {
        val body = buildJsonObject {
            put("success", false)
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
    fun `successful envelope without data fails closed`() = runTest {
        val body = buildJsonObject {
            put("success", true)
            put("message", "ok")
        }
        val result = assertIs<BootstrapRepositoryResult.Failure>(
            repository(NetworkResult.Success(NetworkResponse(200, body = body))).load(),
        )

        assertEquals(
            BootstrapRepositoryFailure.InvalidPayload("bootstrap_missing_data"),
            result.reason,
        )
    }

    @Test
    fun `blank config version fails closed`() = runTest {
        val result = assertIs<BootstrapRepositoryResult.Failure>(
            repository(successResponse(authenticated = false, configVersion = " ")).load(),
        )

        assertEquals(
            BootstrapRepositoryFailure.InvalidPayload("bootstrap_invalid_config_version"),
            result.reason,
        )
    }

    @Test
    fun `conflicting update flags fail closed`() = runTest {
        val result = assertIs<BootstrapRepositoryResult.Failure>(
            repository(successResponse(authenticated = false, required = true, optional = true)).load(),
        )

        assertEquals(
            BootstrapRepositoryFailure.InvalidPayload("bootstrap_conflicting_update_policy"),
            result.reason,
        )
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

    private fun repository(result: NetworkResult): RemoteBootstrapRepository =
        RemoteBootstrapRepository(NetworkDataSource { result })

    private fun successResponse(
        authenticated: Boolean,
        configVersion: String = "1",
        required: Boolean = false,
        optional: Boolean = false,
    ): NetworkResult.Success = NetworkResult.Success(
        NetworkResponse(
            statusCode = 200,
            body = buildJsonObject {
                put("success", true)
                put("message", "Partner bootstrap completed")
                put("data", buildJsonObject {
                    put("config", buildJsonObject {
                        put("version", configVersion)
                        put("maintenance", buildJsonObject {
                            put("enabled", false)
                            put("title", "Maintenance")
                            put("message", "Try later")
                        })
                        put("update", buildJsonObject {
                            put("required", required)
                            put("optional", optional)
                            put("minimumVersion", "1.0.0")
                            put("latestVersion", "1.1.0")
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
                            put("templateId", "partner_login_template")
                            put("templateType", "form_template")
                            put("endpoint", "/api/v1/partner/sdui/registry/partner_login")
                            put("method", "GET")
                            put("authentication", "NONE")
                            put("transition", "RESET")
                            put("restorePolicy", "CACHE_FIRST")
                        })
                    })
                })
                put("traceId", "req-test")
            },
        ),
    )
}
