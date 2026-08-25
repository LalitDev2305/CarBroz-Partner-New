package com.carbroz.runtime.action

import com.carbroz.foundation.capabilities.GenericCapabilityRequest
import com.carbroz.runtime.binding.BindingContext
import com.carbroz.runtime.binding.BindingResolutionError
import com.carbroz.runtime.form.FormStore
import com.carbroz.runtime.sdui.model.Command
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

    data class Capability(
        val request: GenericCapabilityRequest,
    ) : PreparedAction
}

sealed interface ActionPreparationResult {
    data class Success(val action: PreparedAction) : ActionPreparationResult
    data object FormInvalid : ActionPreparationResult
    data class BindingFailure(val error: BindingResolutionError) : ActionPreparationResult
    data class UnsupportedCommand(val command: Command) : ActionPreparationResult
    data class DefinitionRejectedCommand(val command: Command) : ActionPreparationResult
}

/**
 * Converts trusted semantic commands into fully resolved execution intents through the immutable registry.
 * This boundary performs no HTTP, navigation, persistence or platform work.
 */
class ActionPreparer(
    private val registry: ActionRegistry,
) {
    fun prepare(command: Command, context: ActionPreparationContext): ActionPreparationResult {
        val definition = registry.find(command.kind)
            ?: return ActionPreparationResult.UnsupportedCommand(command)
        return definition.prepare(command, context)
            ?: ActionPreparationResult.DefinitionRejectedCommand(command)
    }
}
