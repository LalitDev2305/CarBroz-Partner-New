package com.carbroz.runtime.action

import com.carbroz.runtime.binding.BindingResolutionResult
import com.carbroz.runtime.binding.BindingResolver
import com.carbroz.runtime.sdui.model.Command
import com.carbroz.runtime.sdui.model.CommandKind
import com.carbroz.runtime.sdui.model.FormCommand
import com.carbroz.runtime.sdui.model.LocalStateCommand
import com.carbroz.runtime.sdui.model.PresentationCommand
import com.carbroz.runtime.sdui.model.SduiNavigationCommand
import kotlinx.serialization.json.JsonObject

class NavigationActionDefinition : ActionDefinition {
    override val kind: CommandKind = SduiNavigationCommand.KIND

    override fun prepare(command: Command, context: ActionPreparationContext): ActionPreparationResult? {
        val navigation = command as? SduiNavigationCommand ?: return null
        return ActionPreparationResult.Success(
            PreparedAction.Navigation(navigation.operation, navigation.targetNavigationId),
        )
    }
}

class PresentationActionDefinition(
    private val bindingResolver: BindingResolver = BindingResolver(),
) : ActionDefinition {
    override val kind: CommandKind = PresentationCommand.KIND

    override fun prepare(command: Command, context: ActionPreparationContext): ActionPreparationResult? {
        val presentation = command as? PresentationCommand ?: return null
        return when (val resolution = bindingResolver.resolve(JsonObject(presentation.properties), context.bindings)) {
            is BindingResolutionResult.Failure -> ActionPreparationResult.BindingFailure(resolution.error)
            is BindingResolutionResult.Success -> {
                val properties = resolution.value as? JsonObject ?: return null
                ActionPreparationResult.Success(
                    PreparedAction.Presentation(
                        operation = presentation.operation,
                        kind = presentation.presentationKind,
                        id = presentation.id,
                        properties = properties,
                    ),
                )
            }
        }
    }
}

class LocalStateActionDefinition(
    private val bindingResolver: BindingResolver = BindingResolver(),
) : ActionDefinition {
    override val kind: CommandKind = LocalStateCommand.KIND

    override fun prepare(command: Command, context: ActionPreparationContext): ActionPreparationResult? {
        val local = command as? LocalStateCommand ?: return null
        return when (val resolution = bindingResolver.resolve(JsonObject(local.values), context.bindings)) {
            is BindingResolutionResult.Failure -> ActionPreparationResult.BindingFailure(resolution.error)
            is BindingResolutionResult.Success -> {
                val values = resolution.value as? JsonObject ?: return null
                ActionPreparationResult.Success(PreparedAction.LocalState(values.toMap()))
            }
        }
    }
}

class FormActionDefinition : ActionDefinition {
    override val kind: CommandKind = FormCommand.KIND

    override fun prepare(command: Command, context: ActionPreparationContext): ActionPreparationResult? {
        val form = command as? FormCommand ?: return null
        if (context.form == null) return ActionPreparationResult.DefinitionRejectedCommand(command)
        return ActionPreparationResult.Success(PreparedAction.Form(form.operation))
    }
}
