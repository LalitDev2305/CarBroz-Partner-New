package com.carbroz.runtime.action

import com.carbroz.runtime.binding.BindingContext
import com.carbroz.runtime.binding.BindingResolutionError
import com.carbroz.runtime.binding.BindingResolutionResult
import com.carbroz.runtime.binding.BindingResolver
import com.carbroz.runtime.form.FormStore
import com.carbroz.runtime.sdui.model.Command
import com.carbroz.runtime.sdui.model.RequestCommand
import com.carbroz.runtime.sdui.model.RequestMethod
import com.carbroz.runtime.sdui.model.ScreenDestination
import kotlinx.serialization.json.JsonObject

/** Runtime data available when a trusted SDUI command is activated. */
data class ActionPreparationContext(
    val bindings: BindingContext,
    val form: FormStore? = null,
)

sealed interface PreparedAction {
    data class Request(
        val method: RequestMethod,
        val endpoint: String,
        val destination: ScreenDestination,
        val payload: JsonObject,
    ) : PreparedAction
}

sealed interface ActionPreparationResult {
    data class Success(val action: PreparedAction) : ActionPreparationResult
    data object FormInvalid : ActionPreparationResult
    data class BindingFailure(val error: BindingResolutionError) : ActionPreparationResult
    data class UnsupportedCommand(val command: Command) : ActionPreparationResult
}

/**
 * Converts trusted semantic commands into fully resolved execution intents.
 * This boundary performs no HTTP, navigation, persistence or platform work.
 */
class ActionPreparer(
    private val bindingResolver: BindingResolver = BindingResolver(),
) {
    fun prepare(command: Command, context: ActionPreparationContext): ActionPreparationResult = when (command) {
        is RequestCommand -> prepareRequest(command, context)
        else -> ActionPreparationResult.UnsupportedCommand(command)
    }

    private fun prepareRequest(
        command: RequestCommand,
        context: ActionPreparationContext,
    ): ActionPreparationResult {
        if (context.form != null && !context.form.validate()) {
            return ActionPreparationResult.FormInvalid
        }

        return when (val resolution = bindingResolver.resolve(JsonObject(command.payload), context.bindings)) {
            is BindingResolutionResult.Failure -> ActionPreparationResult.BindingFailure(resolution.error)
            is BindingResolutionResult.Success -> {
                val payload = resolution.value as? JsonObject
                    ?: error("Request payload normalization must always resolve to JsonObject")
                ActionPreparationResult.Success(
                    PreparedAction.Request(
                        method = command.method,
                        endpoint = command.endpoint,
                        destination = command.destination,
                        payload = payload,
                    ),
                )
            }
        }
    }
}
