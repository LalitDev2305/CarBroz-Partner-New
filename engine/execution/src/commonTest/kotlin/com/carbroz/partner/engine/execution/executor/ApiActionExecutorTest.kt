package com.carbroz.partner.engine.execution.executor

import com.carbroz.partner.domain.actions.model.ActionId
import com.carbroz.partner.domain.actions.model.ActionType
import com.carbroz.partner.domain.actions.spec.ActionSpec
import com.carbroz.partner.domain.actions.value.ActionParameters
import com.carbroz.partner.domain.actions.value.ActionValue
import com.carbroz.partner.engine.execution.result.ExecutionResult
import com.carbroz.partner.infrastructure.network.client.NetworkClient
import com.carbroz.partner.infrastructure.network.client.NetworkRequest
import com.carbroz.partner.infrastructure.network.client.NetworkResponse
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ApiActionExecutorTest {

    private class FakeNetworkClient(
        private val responseToReturn: NetworkResponse
    ) : NetworkClient {
        var lastRequest: NetworkRequest? = null
        override suspend fun execute(request: NetworkRequest): NetworkResponse {
            lastRequest = request
            return responseToReturn
        }
    }

    @Test
    fun testApiActionExecutionSuccess() = runTest {
        val fakeNetwork = FakeNetworkClient(NetworkResponse(200, """{"status":"ok"}"""))
        val executor = ApiActionExecutor(fakeNetwork)

        val actionSpec = ActionSpec.create(
            id = ActionId("act_1"),
            type = ActionType.API_REQUEST,
            parameters = ActionParameters.create(mapOf("endpoint" to ActionValue.Text("/api/v1/test")))
        )

        val result = executor.execute(actionSpec)
        assertTrue(result is ExecutionResult.Success)
        assertEquals(ActionValue.Text("""{"status":"ok"}"""), (result as ExecutionResult.Success).output)
        assertEquals("/api/v1/test", fakeNetwork.lastRequest?.url)
    }

    @Test
    fun testApiActionMissingEndpointFails() = runTest {
        val fakeNetwork = FakeNetworkClient(NetworkResponse(200, "{}"))
        val executor = ApiActionExecutor(fakeNetwork)

        val actionSpec = ActionSpec.create(
            id = ActionId("act_1"),
            type = ActionType.API_REQUEST,
            parameters = ActionParameters.EMPTY
        )

        val result = executor.execute(actionSpec)
        assertTrue(result is ExecutionResult.Failure)
    }
}
