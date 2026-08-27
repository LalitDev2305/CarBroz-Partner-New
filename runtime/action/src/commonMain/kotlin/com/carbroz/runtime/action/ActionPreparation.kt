package com.carbroz.runtime.action

import com.carbroz.foundation.capabilities.GenericCapabilityRequest
import com.carbroz.runtime.binding.BindingContext
import com.carbroz.runtime.binding.BindingResolutionError
import com.carbroz.runtime.form.FormStore
import com.carbroz.runtime.sdui.model.Command
import com.carbroz.runtime.sdui.model.FormOperation
import com.carbroz.runtime.sdui.model.NavigationOperation
import com.carbroz.runtime.sdui.model.PresentationKind
import com.carbroz.runtime.sdui.model.PresentationOperation
import com.carbroz.runtime.sdui.model.RequestAuthentication
import com.carbroz.runtime.sdui.model.RequestMethod
import com.carbroz.runtime.sdui.model.RequestResponseMode
import com.carbroz.runtime.sdui.model.ScreenDestination
import com.carbroz.runtime.sdui.model.ScreenTransition
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject

/** Runtime data available when a trusted SDUI command is activated. */
data class ActionPreparationContext(
    val bindings: BindingContext,
    val form: FormStore? = null,
)

/** Fully validated client-owned execution intents. */
sealed interface PreparedAction {
    data class Request(
        val method: RequestMethod,
        val endpoint: String,
        val destination: ScreenDestination?,
        val payload: JsonObject,
        val authentication: RequestAuthentication = RequestAuthentication.SESSION,
        val responseMode: RequestResponseMode = RequestResponseMode.SCREEN,
        val transition: ScreenTransition = ScreenTransition.PUSH,
        val backStackKey: String? = null,
    ) : PreparedAction

    data class Capability(val request: GenericCapabilityRequest) : PreparedAction

    data class Navigation(
        val operation: NavigationOperation,
        val targetNavigationId: String? = null,
    ) : PreparedAction

    data class Presentation(
        val operation: PresentationOperation,
        val kind: PresentationKind,
        val id: String,
        val properties: JsonObject,
    ) : PreparedAction

    data class LocalState(val values: Map<String, JsonElement>) : PreparedAction
    data class Form(val operation: FormOperation) : PreparedAction
}

sealed interface ActionPreparationResult {
    data class Success(val action: PreparedAction) : ActionPreparationResult
    data object FormInvalid : ActionPreparationResult
    data class BindingFailure(val error: BindingResolutionError) : ActionPreparationResult
    data class UnsupportedCommand(val command: Command) : ActionPreparationResult
    data class DefinitionRejectedCommand(val command: Command) : ActionPreparationResult
}

/** Converts trusted semantic commands into fully resolved execution intents through the immutable registry. */
class ActionPreparer(private val registry: ActionRegistry) {
    fun prepare(command: Command, context: ActionPreparationContext): ActionPreparationResult {
        val definition = registry.find(command.kind)
            ?: return ActionPreparationResult.UnsupportedCommand(command)
        return definition.prepare(command, context)
            ?: ActionPreparationResult.DefinitionRejectedCommand(command)
    }
}
