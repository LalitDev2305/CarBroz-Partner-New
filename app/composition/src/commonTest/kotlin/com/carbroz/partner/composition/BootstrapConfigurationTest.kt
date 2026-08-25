package com.carbroz.partner.composition

import com.carbroz.data.network.NetworkAuthentication
import com.carbroz.data.network.NetworkCachePolicy
import com.carbroz.data.network.NetworkDataSource
import com.carbroz.data.network.NetworkFailure
import com.carbroz.data.network.NetworkMethod
import com.carbroz.data.network.NetworkRequest
import com.carbroz.data.network.NetworkResponse
import com.carbroz.data.network.NetworkResult
import com.carbroz.runtime.application.startup.StartupFailure
import com.carbroz.runtime.application.startup.StartupTaskResult
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
    fun `successful bootstrap resolves allow-listed semantic route`() = runTest {
        var capturedRequest: NetworkRequest? = null
        val routes = BootstrapRouteStore()
        val task = BootstrapConfigurationStartupTask(
            network = NetworkDataSource { request ->
                capturedRequest = request
                NetworkResult.Success(
                    NetworkResponse(
                        statusCode = 200,
                        body = buildJsonObject { put("nextDestination", "reference-sdui") },
                    ),
                )
            },
            routes = routes,
        )

        assertEquals(StartupTaskResult.Success, task.execute())
        assertEquals(BootstrapRoute.Reference, routes.current())
        assertEquals(NetworkMethod.GET, capturedRequest?.method)
        assertEquals("/api/v1/app", capturedRequest?.endpoint?.value)
        assertEquals(NetworkAuthentication.NONE, capturedRequest?.authentication)
        assertIs<NetworkCachePolicy.NetworkFirst>(capturedRequest?.cachePolicy)
    }

    @Test
    fun `unsupported server destination is rejected without mutating route`() = runTest {
        val routes = BootstrapRouteStore()
        val task = BootstrapConfigurationStartupTask(
            network = successfulNetwork("remote-class-name"),
            routes = routes,
        )

        val failure = assertIs<StartupTaskResult.Failure>(task.execute())
        val reason = assertIs<StartupFailure.Expected>(failure.reason)
        assertEquals("bootstrap_unsupported_destination", reason.code)
        assertEquals(false, reason.recoverable)
        assertNull(routes.current())
    }

    @Test
    fun `offline bootstrap is recoverable`() = runTest {
        val task = BootstrapConfigurationStartupTask(
            network = NetworkDataSource { NetworkResult.Failure(NetworkFailure.Offline) },
            routes = BootstrapRouteStore(),
        )

        val failure = assertIs<StartupTaskResult.Failure>(task.execute())
        val reason = assertIs<StartupFailure.Expected>(failure.reason)
        assertEquals("bootstrap_offline", reason.code)
        assertEquals(true, reason.recoverable)
    }

    @Test
    fun `malformed successful response fails closed`() = runTest {
        val routes = BootstrapRouteStore()
        val task = BootstrapConfigurationStartupTask(
            network = NetworkDataSource {
                NetworkResult.Success(
                    NetworkResponse(
                        statusCode = 200,
                        body = JsonPrimitive("not-an-object"),
                    ),
                )
            },
            routes = routes,
        )

        val failure = assertIs<StartupTaskResult.Failure>(task.execute())
        val reason = assertIs<StartupFailure.Expected>(failure.reason)
        assertEquals("bootstrap_missing_object", reason.code)
        assertNull(routes.current())
    }

    private fun successfulNetwork(destination: String): NetworkDataSource = NetworkDataSource {
        NetworkResult.Success(
            NetworkResponse(
                statusCode = 200,
                body = buildJsonObject { put("nextDestination", destination) },
            ),
        )
    }
}
