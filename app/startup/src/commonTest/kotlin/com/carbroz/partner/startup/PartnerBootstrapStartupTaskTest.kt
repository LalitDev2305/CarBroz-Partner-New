package com.carbroz.partner.startup

import com.carbroz.data.network.NetworkDataSource
import com.carbroz.data.network.NetworkFailure
import com.carbroz.data.network.NetworkResponse
import com.carbroz.data.network.NetworkResult
import com.carbroz.feature.dynamic.DynamicScreenInstructionCodec
import com.carbroz.foundation.session.SessionProvider
import com.carbroz.foundation.session.SessionState
import com.carbroz.runtime.application.startup.StartupFailure
import com.carbroz.runtime.application.startup.StartupResolution
import com.carbroz.runtime.application.startup.StartupTaskResult
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class PartnerBootstrapStartupTaskTest {
    @Test
    fun `guest bootstrap resolves ready through client and policy`() = runTest {
        val task = task(authenticated = false, localSession = SessionState.SignedOut)

        val result = assertIs<StartupTaskResult.Resolved>(task.execute())
        assertIs<StartupResolution.Ready>(result.resolution)
    }

    @Test
    fun `backend and canonical local session disagreement fails closed`() = runTest {
        val task = task(authenticated = true, localSession = SessionState.SignedOut)

        assertEquals(
            StartupTaskResult.Failure(
                StartupFailure.Expected("bootstrap_session_mismatch", recoverable = false),
            ),
            task.execute(),
        )
    }

    @Test
    fun `unresolved bootstrap 401 remains recoverable for a later startup retry`() = runTest {
        val client = PartnerBootstrapClient(
            NetworkDataSource { NetworkResult.Failure(NetworkFailure.Http(401)) },
        )
        val task = PartnerBootstrapStartupTask(
            client = client,
            policyEvaluator = PartnerBootstrapPolicyEvaluator(DynamicScreenInstructionCodec()),
            sessionProvider = SessionProvider { SessionState.SignedOut },
        )

        assertEquals(
            StartupTaskResult.Failure(
                StartupFailure.Expected("bootstrap_http_401", recoverable = true),
            ),
            task.execute(),
        )
    }

    @Test
    fun `non recoverable client contract failure stays non recoverable`() = runTest {
        val client = PartnerBootstrapClient(
            NetworkDataSource {
                NetworkResult.Success(NetworkResponse(statusCode = 200, body = buildJsonObject { put("success", true) }))
            },
        )
        val task = PartnerBootstrapStartupTask(
            client = client,
            policyEvaluator = PartnerBootstrapPolicyEvaluator(DynamicScreenInstructionCodec()),
            sessionProvider = SessionProvider { SessionState.SignedOut },
        )

        val failure = assertIs<StartupTaskResult.Failure>(task.execute())
        assertEquals(false, failure.reason.recoverable)
    }

    private fun task(
        authenticated: Boolean,
        localSession: SessionState,
    ): PartnerBootstrapStartupTask {
        val client = PartnerBootstrapClient(
            NetworkDataSource {
                NetworkResult.Success(
                    NetworkResponse(
                        statusCode = 200,
                        body = response(authenticated),
                    ),
                )
            },
        )
        return PartnerBootstrapStartupTask(
            client = client,
            policyEvaluator = PartnerBootstrapPolicyEvaluator(DynamicScreenInstructionCodec()),
            sessionProvider = SessionProvider { localSession },
        )
    }

    private fun response(authenticated: Boolean) = buildJsonObject {
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
                put("authenticated", authenticated)
                put("nextScreen", buildJsonObject {
                    put("screenId", "partner_login")
                    put("templateId", "partner_login_template")
                    put("templateType", "form_template")
                    put("endpoint", "/api/v1/partner/sdui/registry/partner_login")
                    put("method", "GET")
                    put("authentication", "NONE")
                })
            })
        })
    }
}
