package com.carbroz.partner.engine.execution.executor

import com.carbroz.partner.engine.execution.action.ActionSpec
import com.carbroz.partner.engine.execution.action.ActionType
import com.carbroz.partner.engine.execution.action.ActionValue
import com.carbroz.partner.engine.execution.result.ExecutionFailure
import com.carbroz.partner.engine.execution.result.ExecutionResult
import com.carbroz.partner.infrastructure.network.client.NetworkClient
import com.carbroz.partner.infrastructure.network.client.NetworkRequest

public class ApiActionExecutor(
    private val networkClient: NetworkClient
) : ActionExecutor {

    override val supportedType: ActionType = ActionType.API_REQUEST

    override suspend fun execute(action: ActionSpec): ExecutionResult {
        if (action.type != supportedType) {
            return ExecutionResult.Failure(
                ExecutionFailure(
                    code = ExecutionFailure.FailureCode.UNREGISTERED_ACTION_TYPE,
                    message = "ApiActionExecutor unsupported type: '${action.type.rawValue}'"
                )
            )
        }

        val endpointParam = action.parameters.get("endpoint")
        val endpoint = (endpointParam as? ActionValue.Text)?.value
        if (endpoint.isNullOrBlank()) {
            return ExecutionResult.Failure(
                ExecutionFailure(
                    code = ExecutionFailure.FailureCode.EXECUTOR_FAILED,
                    message = "Missing required 'endpoint' parameter for API_REQUEST"
                )
            )
        }

        val authPolicyParam = (action.parameters.get("auth_policy") as? ActionValue.Text)?.value
        val parsedAuthPolicy = when (authPolicyParam?.lowercase()) {
            "none" -> com.carbroz.partner.infrastructure.network.client.AuthPolicy.NONE
            "required" -> com.carbroz.partner.infrastructure.network.client.AuthPolicy.REQUIRED
            else -> com.carbroz.partner.infrastructure.network.client.AuthPolicy.OPTIONAL
        }

        return try {
            val response = networkClient.execute(
                NetworkRequest(
                    url = endpoint,
                    method = "POST",
                    authPolicy = parsedAuthPolicy
                )
            )
            if (response.isSuccessful) {
                ExecutionResult.Success(
                    output = ActionValue.Text(response.bodyJson)
                )
            } else {
                ExecutionResult.Failure(
                    ExecutionFailure(
                        code = ExecutionFailure.FailureCode.EXECUTOR_FAILED,
                        message = "API request failed with status code ${response.statusCode}"
                    )
                )
            }
        } catch (e: Exception) {
            ExecutionResult.Failure(
                ExecutionFailure(
                    code = ExecutionFailure.FailureCode.EXECUTOR_FAILED,
                    message = e.message ?: "Network error"
                )
            )
        }
    }
}
