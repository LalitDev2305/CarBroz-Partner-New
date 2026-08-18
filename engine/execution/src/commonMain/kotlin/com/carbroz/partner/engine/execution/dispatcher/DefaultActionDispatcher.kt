package com.carbroz.partner.engine.execution.dispatcher

import com.carbroz.partner.core.observability.logger.BoundLogger
import com.carbroz.partner.core.observability.logger.StructuredLogger
import com.carbroz.partner.core.observability.model.LogCategory
import com.carbroz.partner.domain.actions.spec.ActionSpec
import com.carbroz.partner.domain.actions.value.ActionParameters
import com.carbroz.partner.domain.actions.value.ActionValue
import com.carbroz.partner.engine.execution.binding.BindingResolver
import com.carbroz.partner.engine.execution.binding.BindingResult
import com.carbroz.partner.engine.execution.binding.BindingScope
import com.carbroz.partner.engine.execution.executor.ActionRegistry
import com.carbroz.partner.engine.execution.result.ExecutionFailure
import com.carbroz.partner.engine.execution.result.ExecutionResult
import kotlin.coroutines.cancellation.CancellationException

/**
 * Standard implementation of ActionDispatcher orchestrating lookup, recursive parameter binding resolution, and execution.
 */
class DefaultActionDispatcher(
    private val registry: ActionRegistry,
    private val bindingResolver: BindingResolver,
    logger: StructuredLogger
) : ActionDispatcher {

    private val boundLogger: BoundLogger = logger.withSource("DefaultActionDispatcher")

    override suspend fun dispatch(
        action: ActionSpec,
        scope: BindingScope?
    ): ExecutionResult {
        val executor = registry.getExecutor(action.type)
            ?: return ExecutionResult.Failure(
                ExecutionFailure(
                    code = ExecutionFailure.FailureCode.UNREGISTERED_ACTION_TYPE,
                    message = "No ActionExecutor registered for ActionType '${action.type.rawValue}'"
                )
            )

        val containsBindings = hasBindingsRecursively(action.parameters)
        if (containsBindings && scope == null) {
            return ExecutionResult.Failure(
                ExecutionFailure(
                    code = ExecutionFailure.FailureCode.BINDING_RESOLUTION_FAILED,
                    message = "Action contains expressions but BindingScope is null"
                )
            )
        }

        val resolvedParameters = if (containsBindings && scope != null) {
            when (val result = resolveParametersRecursively(action.parameters, scope)) {
                is ParameterResolution.Success -> result.parameters
                is ParameterResolution.Failure -> return ExecutionResult.Failure(
                    ExecutionFailure(
                        code = ExecutionFailure.FailureCode.BINDING_RESOLUTION_FAILED,
                        message = result.message
                    )
                )
            }
        } else {
            action.parameters
        }

        val resolvedAction = ActionSpec.create(
            id = action.id,
            type = action.type,
            parameters = resolvedParameters,
            metadata = action.metadata
        )

        boundLogger.info(
            sourceFunction = "dispatch",
            category = LogCategory.EXECUTION,
            event = "action_dispatch_start",
            message = "Dispatching action id='${action.id.value}', type='${action.type.rawValue}'"
        )

        return try {
            val result = executor.execute(resolvedAction)
            when (result) {
                is ExecutionResult.Success -> boundLogger.info(
                    sourceFunction = "dispatch",
                    category = LogCategory.EXECUTION,
                    event = "action_execution_success",
                    message = "Action execution succeeded id='${action.id.value}'"
                )
                is ExecutionResult.Failure -> boundLogger.info(
                    sourceFunction = "dispatch",
                    category = LogCategory.EXECUTION,
                    event = "action_execution_failure",
                    message = "Action execution failed id='${action.id.value}', code='${result.failure.code}'"
                )
            }
            result
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            boundLogger.error(
                sourceFunction = "dispatch",
                category = LogCategory.EXECUTION,
                event = "action_executor_unexpected_error",
                message = "Unexpected executor exception for action id='${action.id.value}'"
            )
            ExecutionResult.Failure(
                ExecutionFailure(
                    code = ExecutionFailure.FailureCode.EXECUTOR_FAILED,
                    message = "Action executor encountered an unexpected error"
                )
            )
        }
    }

    private fun hasBindingsRecursively(parameters: ActionParameters): Boolean {
        for (entry in parameters.entries) {
            if (hasBindingsValueRecursively(entry.value)) return true
        }
        return false
    }

    private fun hasBindingsValueRecursively(value: ActionValue): Boolean {
        return when (value) {
            is ActionValue.Binding -> true
            is ActionValue.Object -> value.properties.values.any { hasBindingsValueRecursively(it) }
            is ActionValue.List -> value.items.any { hasBindingsValueRecursively(it) }
            else -> false
        }
    }

    private fun resolveParametersRecursively(
        parameters: ActionParameters,
        scope: BindingScope
    ): ParameterResolution {
        val map = parameters.entries
        val resolvedMap = mutableMapOf<String, ActionValue>()
        for (entry in map) {
            when (val res = resolveValueRecursively(entry.value, scope)) {
                is ValueResolution.Success -> resolvedMap[entry.key] = res.value
                is ValueResolution.Failure -> return ParameterResolution.Failure(res.message)
            }
        }
        return ParameterResolution.Success(ActionParameters.create(resolvedMap))
    }

    private fun resolveValueRecursively(
        value: ActionValue,
        scope: BindingScope
    ): ValueResolution {
        return when (value) {
            is ActionValue.Binding -> {
                when (val result = bindingResolver.resolve(value.expression, scope)) {
                    is BindingResult.Success -> ValueResolution.Success(result.value)
                    is BindingResult.Failure -> ValueResolution.Failure(result.reason)
                }
            }
            is ActionValue.Object -> {
                val resolvedMap = mutableMapOf<String, ActionValue>()
                for ((k, v) in value.properties) {
                    when (val res = resolveValueRecursively(v, scope)) {
                        is ValueResolution.Success -> resolvedMap[k] = res.value
                        is ValueResolution.Failure -> return res
                    }
                }
                ValueResolution.Success(ActionValue.Object.create(resolvedMap))
            }
            is ActionValue.List -> {
                val resolvedList = mutableListOf<ActionValue>()
                for (item in value.items) {
                    when (val res = resolveValueRecursively(item, scope)) {
                        is ValueResolution.Success -> resolvedList.add(res.value)
                        is ValueResolution.Failure -> return res
                    }
                }
                ValueResolution.Success(ActionValue.List.create(resolvedList))
            }
            else -> ValueResolution.Success(value)
        }
    }

    private sealed interface ParameterResolution {
        data class Success(val parameters: ActionParameters) : ParameterResolution
        data class Failure(val message: String) : ParameterResolution
    }

    private sealed interface ValueResolution {
        data class Success(val value: ActionValue) : ValueResolution
        data class Failure(val message: String) : ValueResolution
    }
}
