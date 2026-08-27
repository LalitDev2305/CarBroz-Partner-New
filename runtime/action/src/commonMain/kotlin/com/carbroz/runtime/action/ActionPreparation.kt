package com.carbroz.runtime.action

import com.carbroz.foundation.capabilities.GenericCapabilityRequest
import com.carbroz.runtime.binding.BindingContext
import com.carbroz.runtime.binding.BindingResolutionError
import com.carbroz.runtime.binding.BindingResolutionResult
import com.carbroz.runtime.binding.BindingResolver
import com.carbroz.runtime.form.FormStore
import com.carbroz.runtime.sdui.model.BackgroundNetworkRequirement
import com.carbroz.runtime.sdui.model.BackgroundOperation
import com.carbroz.runtime.sdui.model.BackgroundWorkKind
import com.carbroz.runtime.sdui.model.Command
import com.carbroz.runtime.sdui.model.ConditionalCommand
import com.carbroz.runtime.sdui.model.FormOperation
import com.carbroz.runtime.sdui.model.NavigationOperation
import com.carbroz.runtime.sdui.model.PresentationKind
import com.carbroz.runtime.sdui.model.PresentationOperation
import com.carbroz.runtime.sdui.model.RequestAuthentication
import com.carbroz.runtime.sdui.model.RequestMethod
import com.carbroz.runtime.sdui.model.RequestResponseMode
import com.carbroz.runtime.sdui.model.ScreenDestination
import com.carbroz.runtime.sdui.model.ScreenTransition
import com.carbroz.runtime.sdui.model.SequenceCommand
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull

data class ActionPreparationContext(
    val bindings: BindingContext,
    val form: FormStore? = null,
)

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
    data class Navigation(val operation: NavigationOperation, val targetNavigationId: String? = null) : PreparedAction
    data class Presentation(
        val operation: PresentationOperation,
        val kind: PresentationKind,
        val id: String,
        val properties: JsonObject,
    ) : PreparedAction
    data class LocalState(val values: Map<String, JsonElement>) : PreparedAction
    data class Form(val operation: FormOperation) : PreparedAction
    data class Background(
        val operation: BackgroundOperation,
        val id: String,
        val workKind: BackgroundWorkKind,
        val earliestStartDelayMillis: Long,
        val networkRequirement: BackgroundNetworkRequirement,
        val requiresCharging: Boolean,
        val title: String?,
        val description: String?,
        val input: JsonObject,
    ) : PreparedAction
    data class Sequence(val actions: List<PreparedAction>) : PreparedAction
}

sealed interface ActionPreparationResult {
    data class Success(val action: PreparedAction) : ActionPreparationResult
    data object FormInvalid : ActionPreparationResult
    data class BindingFailure(val error: BindingResolutionError) : ActionPreparationResult
    data class UnsupportedCommand(val command: Command) : ActionPreparationResult
    data class DefinitionRejectedCommand(val command: Command) : ActionPreparationResult
    data class InvalidCondition(val command: ConditionalCommand) : ActionPreparationResult
    data class InvalidSequence(val command: SequenceCommand, val reason: String) : ActionPreparationResult
}

class ActionPreparer(
    private val registry: ActionRegistry,
    private val bindingResolver: BindingResolver = BindingResolver(),
) {
    fun prepare(command: Command, context: ActionPreparationContext): ActionPreparationResult = when (command) {
        is SequenceCommand -> prepareSequence(command, context)
        is ConditionalCommand -> prepareConditional(command, context)
        else -> prepareRegistered(command, context)
    }

    private fun prepareRegistered(command: Command, context: ActionPreparationContext): ActionPreparationResult {
        val definition = registry.find(command.kind)
            ?: return ActionPreparationResult.UnsupportedCommand(command)
        return definition.prepare(command, context)
            ?: ActionPreparationResult.DefinitionRejectedCommand(command)
    }

    private fun prepareSequence(command: SequenceCommand, context: ActionPreparationContext): ActionPreparationResult {
        val prepared = mutableListOf<PreparedAction>()
        command.commands.forEachIndexed { index, child ->
            when (val result = prepare(child, context)) {
                is ActionPreparationResult.Success -> {
                    if (index != command.commands.lastIndex && result.action.isTerminalTransition()) {
                        return ActionPreparationResult.InvalidSequence(
                            command,
                            "screen/navigation transition must be the final sequence action",
                        )
                    }
                    prepared += result.action
                }
                else -> return result
            }
        }
        return ActionPreparationResult.Success(PreparedAction.Sequence(prepared))
    }

    private fun prepareConditional(command: ConditionalCommand, context: ActionPreparationContext): ActionPreparationResult {
        val resolved = when (val result = bindingResolver.resolve(command.condition, context.bindings)) {
            is BindingResolutionResult.Failure -> return ActionPreparationResult.BindingFailure(result.error)
            is BindingResolutionResult.Success -> result.value
        }
        val condition = (resolved as? JsonPrimitive)?.booleanOrNull
            ?: return ActionPreparationResult.InvalidCondition(command)
        val branch = if (condition) command.whenTrue else command.whenFalse
            ?: return ActionPreparationResult.Success(PreparedAction.Sequence(emptyList()))
        return prepare(branch, context)
    }

    private fun PreparedAction.isTerminalTransition(): Boolean = when (this) {
        is PreparedAction.Navigation -> true
        is PreparedAction.Request -> responseMode == RequestResponseMode.SCREEN
        is PreparedAction.Sequence -> actions.lastOrNull()?.isTerminalTransition() == true
        else -> false
    }
}
