package com.carbroz.partner.composition

import com.carbroz.data.network.NetworkAuthentication
import com.carbroz.data.network.NetworkDataSource
import com.carbroz.data.network.NetworkMethod
import com.carbroz.data.network.NetworkRequest
import com.carbroz.data.network.NetworkResponse
import com.carbroz.data.network.NetworkResult
import com.carbroz.runtime.action.PreparedAction
import com.carbroz.runtime.sdui.model.NodeType
import com.carbroz.runtime.sdui.model.RequestMethod
import com.carbroz.runtime.sdui.model.ScreenDestination
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertSame

class NetworkActionExecutorTest {
    @Test
    fun preparedRequestUsesCanonicalSessionNetworkPath() = runTest {
        val expected = NetworkResult.Success(NetworkResponse(statusCode = 204))
        val dataSource = CapturingNetworkDataSource(expected)
        val executor = NetworkActionExecutor(dataSource)
        val payload = buildJsonObject { put("bookingId", JsonPrimitive("B-42")) }
        val action = PreparedAction.Request(
            method = RequestMethod.POST,
            endpoint = "/v1/actions/booking",
            destination = ScreenDestination("next", "form", NodeType("FORM")),
            payload = payload,
        )

        val result = executor.execute(action)

        assertSame(expected, result)
        assertEquals(NetworkMethod.POST, dataSource.request?.method)
        assertEquals("/v1/actions/booking", dataSource.request?.endpoint?.value)
        assertEquals(payload, dataSource.request?.payload)
        assertEquals(NetworkAuthentication.SESSION, dataSource.request?.authentication)
    }

    @Test
    fun allSemanticMethodsMapWithoutScreenSpecificClients() = runTest {
        RequestMethod.entries.forEach { method ->
            val dataSource = CapturingNetworkDataSource(NetworkResult.Success(NetworkResponse(200)))
            val executor = NetworkActionExecutor(dataSource)
            executor.execute(
                PreparedAction.Request(
                    method = method,
                    endpoint = "/v1/generic",
                    destination = ScreenDestination("next", "form", NodeType("FORM")),
                    payload = buildJsonObject {},
                ),
            )

            assertEquals(method.name, dataSource.request?.method?.name)
        }
    }

    private class CapturingNetworkDataSource(
        private val result: NetworkResult,
    ) : NetworkDataSource {
        var request: NetworkRequest? = null

        override suspend fun execute(request: NetworkRequest): NetworkResult {
            this.request = request
            return result
        }
    }
}
