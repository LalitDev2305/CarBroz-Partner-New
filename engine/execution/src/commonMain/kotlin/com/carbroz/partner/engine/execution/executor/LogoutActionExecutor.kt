package com.carbroz.partner.engine.execution.executor

import com.carbroz.partner.engine.execution.action.ActionSpec
import com.carbroz.partner.engine.execution.action.ActionType
import com.carbroz.partner.domain.session.operation.ClearSession
import com.carbroz.partner.engine.execution.result.ExecutionFailure
import com.carbroz.partner.engine.execution.result.ExecutionResult

public class LogoutActionExecutor(
    private val clearSession: ClearSession
) : ActionExecutor {

    override val supportedType: ActionType = ActionType.AUTH_LOGOUT

    override suspend fun execute(action: ActionSpec): ExecutionResult {
        return try {
            val cleared = clearSession.execute()
            if (cleared) {
                ExecutionResult.Success()
            } else {
                ExecutionResult.Failure(
                    ExecutionFailure(
                        code = ExecutionFailure.FailureCode.EXECUTOR_FAILED,
                        message = "Logout failed to clear session credentials"
                    )
                )
            }
        } catch (e: Exception) {
            ExecutionResult.Failure(
                ExecutionFailure(
                    code = ExecutionFailure.FailureCode.EXECUTOR_FAILED,
                    message = e.message ?: "Logout execution failed"
                )
            )
        }
    }
}
