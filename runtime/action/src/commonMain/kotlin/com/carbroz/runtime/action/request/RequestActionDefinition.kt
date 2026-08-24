package com.carbroz.runtime.action.request

import com.carbroz.runtime.action.ActionDefinition
import com.carbroz.runtime.action.ActionPreparationContext
import com.carbroz.runtime.action.ActionPreparationResult
import com.carbroz.runtime.action.PreparedAction
import com.carbroz.runtime.binding.BindingResolutionResult
import com.carbroz.runtime.binding.BindingResolver
import com.carbroz.runtime.sdui.model.Command
import com.carbroz.runtime.sdui.model.CommandKind
import com.carbroz.runtime.sdui.model.RequestCommand
import kotlinx.serialization.json.JsonObject

class RequestActionDefinition(
    private val bindingResolver: BindingResolver = BindingResolver(),
) : ActionDefinition {
    override val kind: CommandKind = RequestCommand.KIND

    override fun prepare(
        command: Command,
        context: ActionPreparationContext,
    ): ActionPreparationResult? {
        val request = command as? RequestCommand ?: return null

        if (context.form != null && !context.form.validate()) {
            return ActionPreparationResult.FormInvalid
        }

        return when (val resolution = bindingResolver.resolve(JsonObject(request.payload), context.bindings)) {
            is BindingResolutionResult.Failure -> ActionPreparationResult.BindingFailure(resolution.error)
            is BindingResolutionResult.Success -> {
                val payload = resolution.value as? JsonObject ?: return null
                ActionPreparationResult.Success(
                    PreparedAction.Request(
                        method = request.method,
                        endpoint = request.endpoint,
                        destination = request.destination,
                        payload = payload,
                    ),
                )
            }
        }
    }
}
