package com.carbroz.feature.splash

import com.carbroz.data.network.NetworkAuthentication
import com.carbroz.data.network.NetworkCachePolicy
import com.carbroz.data.network.NetworkDataSource
import com.carbroz.data.network.NetworkFailure
import com.carbroz.data.network.NetworkMethod
import com.carbroz.data.network.NetworkRequest
import com.carbroz.data.network.NetworkResponse
import com.carbroz.data.network.NetworkResult
import com.carbroz.feature.dynamic.DynamicRestorePolicy
import com.carbroz.runtime.application.startup.StartupFailure
import com.carbroz.runtime.application.startup.StartupTaskResult
import com.carbroz.runtime.sdui.model.NodeType
import com.carbroz.runtime.sdui.model.RequestAuthentication
import com.carbroz.runtime.sdui.model.RequestMethod
import com.carbroz.runtime.sdui.model.ScreenTransition
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull

class BootstrapConfigurationTest {
    @Test
    fun `successful bootstrap resolves validated dynamic screen instruction`() = runTest {
        var capturedRequest: NetworkRequest? = null
        val destinations = BootstrapDestinationStore()
        val task = BootstrapConfigurationStartupTask(
            network = NetworkDataSource { request ->
                capturedRequest = request
                successfulBootstrapResponse()
            },
            destinations = destinations,
        )

        assertEquals(StartupTaskResult.Success, task.execute())
        val instruction = destinations.current() ?: error("missing bootstrap instruction")
        assertEquals("screen-entry", instruction.destination.screenId)
        assertEquals("template-form", instruction.destination.templateId)
        assertEquals(NodeType("FORM"), instruction.destination.templateType)
        assertEquals(RequestMethod.GET, instruction.request.method)
        assertEquals("/api/v1/screen/entry", instruction.request.endpoint)
        assertEquals(RequestAuthentication.OPTIONAL_SESSION, instruction.request.authentication)
        assertEquals(ScreenTransition.RESET, instruction.transition)
        assertEquals(DynamicRestorePolicy.CACHE_FIRST, instruction.restorePolicy)
        assertEquals("entry", instruction.backStackKey)
        assertEquals(NetworkMethod.GET, capturedRequest?.method)
        assertEquals("/api/v1/app", capturedRequest?.endpoint?.value)
        assertEquals(NetworkAuthentication.OPTIONAL_SESSION, capturedRequest?.authentication)
        assertEquals(NetworkCachePolicy.NetworkOnly, capturedRequest?.cachePolicy)
    }

    @Test
    fun `invalid dynamic screen instruction is rejected without mutating destination`() = runTest {
        val destinations = BootstrapDestinationStore()
        val task = BootstrapConfigurationStartupTask(
            network = NetworkDataSource { successfulBootstrapResponse(endpoint = "https://untrusted.example/screen") },
            destinations = destinations,
        )
        val failure = assertIs<StartupTaskResult.Failure>(task.execute())
        val reason = assertIs<StartupFailure.Expected>(failure.reason)
        assertEquals("bootstrap_dynamic_instruction_invalid_endpoint", reason.code)
        assertEquals(false, reason.recoverable)
        assertNull(destinations.current())
    }

    @Test
    fun `offline bootstrap is recoverable`() = runTest {
        val task = BootstrapConfigurationStartupTask(
            network = NetworkDataSource { NetworkResult.Failure(NetworkFailure.Offline) },
            destinations = BootstrapDestinationStore(),
        )
        val failure = assertIs<StartupTaskResult.Failure>(task.execute())
        val reason = assertIs<StartupFailure.Expected>(failure.reason)
        assertEquals("bootstrap_offline", reason.code)
        assertEquals(true, reason.recoverable)
    }

    @Test
    fun `malformed successful response fails closed`() = runTest {
        val destinations = BootstrapDestinationStore()
        val task = BootstrapConfigurationStartupTask(
            network = NetworkDataSource {
                NetworkResult.Success(NetworkResponse(statusCode = 200, body = JsonPrimitive("not-an-object")))
            },
            destinations = destinations,
        )
        val failure = assertIs<StartupTaskResult.Failure>(task.execute())
        val reason = assertIs<StartupFailure.Expected>(failure.reason)
        assertEquals("bootstrap_invalid_payload", reason.code)
        assertNull(destinations.current())
    }

    private fun successfulBootstrapResponse(endpoint: String = "/api/v1/screen/entry"): NetworkResult.Success =
        NetworkResult.Success(
            NetworkResponse(
                statusCode = 200,
                body = buildJsonObject {
                    put("nextScreen", buildJsonObject {
                        put("screenId", "screen-entry")
                        put("templateId", "template-form")
                        put("templateType", "FORM")
                        put("endpoint", endpoint)
                        put("method", "GET")
                        put("authentication", "OPTIONAL_SESSION")
                        put("transition", "RESET")
                        put("restorePolicy", "CACHE_FIRST")
                        put("backStackKey", "entry")
                    })
                },
            ),
        )
}
