package com.carbroz.partner.startup

import com.carbroz.data.network.NetworkAuthentication
import com.carbroz.data.network.NetworkDataSource
import com.carbroz.data.network.NetworkFailure
import com.carbroz.data.network.NetworkMethod
import com.carbroz.data.network.NetworkRequest
import com.carbroz.data.network.NetworkResponse
import com.carbroz.data.network.NetworkResult
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class PartnerBootstrapClientTest {
    @Test
    fun `client calls Partner bootstrap with optional session and no manual headers`() = runTest {
        var captured: NetworkRequest? = null
        val client = PartnerBootstrapClient(
            NetworkDataSource { request ->
                captured = request
                successResponse()
            },
        )

        assertIs<PartnerBootstrapClientResult.Success>(client.fetch())
        assertEquals(NetworkMethod.GET, captured?.method)
        assertEquals(PartnerBootstrapClient.ENDPOINT, captured?.endpoint?.value)
        assertEquals(NetworkAuthentication.OPTIONAL_SESSION, captured?.authentication)
        assertTrue(captured?.headers?.isEmpty() == true)
    }

    @Test
    fun `malformed response fails closed`() = runTest {
        val client = PartnerBootstrapClient(
            NetworkDataSource {
                NetworkResult.Success(NetworkResponse(200, body = JsonPrimitive("invalid")))
            },
        )

        val failure = assertIs<PartnerBootstrapClientResult.Failure>(client.fetch())
        assertEquals(
            PartnerBootstrapClientFailure.InvalidPayload("bootstrap_invalid_payload"),
            failure.reason,
        )
    }

    @Test
    fun `network failures remain typed at client boundary`() = runTest {
        val client = PartnerBootstrapClient(
            NetworkDataSource { NetworkResult.Failure(NetworkFailure.Offline) },
        )

        val failure = assertIs<PartnerBootstrapClientResult.Failure>(client.fetch())
        assertEquals(PartnerBootstrapClientFailure.Offline, failure.reason)
    }

    private fun successResponse(): NetworkResult.Success = NetworkResult.Success(
        NetworkResponse(
            statusCode = 200,
            body = buildJsonObject {
                put("success", true)
                put("message", "Partner bootstrap completed")
                put("data", buildJsonObject {
                    put("config", buildJsonObject {
                        put("version", "1")
                        put("maintenance", buildJsonObject { put("enabled", false) })
                        put("update", buildJsonObject {
                            put("required", false)
                            put("optional", false)
                            put("minimumVersion", "1.0.0")
                            put("latestVersion", "1.0.0")
                        })
                        put("features", buildJsonObject {
                            put("registrationEnabled", true)
                            put("individualPartnerEnabled", true)
                            put("organizationPartnerEnabled", true)
                        })
                    })
                    put("startup", buildJsonObject {
                        put("authenticated", false)
                        put("nextScreen", nextScreen())
                    })
                })
                put("traceId", "req-test")
            },
        ),
    )

    private fun nextScreen() = buildJsonObject {
        put("screenId", "partner_login")
        put("templateId", "partner_login_template")
        put("templateType", "form_template")
        put("endpoint", "/api/v1/partner/sdui/registry/partner_login")
        put("method", "GET")
        put("authentication", "NONE")
    }
}
