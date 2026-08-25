package com.carbroz.runtime.action

import com.carbroz.foundation.capabilities.CapabilityKind
import com.carbroz.foundation.capabilities.GenericCapabilityRequest
import com.carbroz.runtime.sdui.model.CapabilityCommand
import com.carbroz.runtime.sdui.model.Command
import com.carbroz.runtime.sdui.model.CommandKind

class CapabilityActionDefinition : ActionDefinition {
    override val kind: CommandKind = CapabilityCommand.KIND

    override fun prepare(
        command: Command,
        context: ActionPreparationContext,
    ): ActionPreparationResult? {
        val capabilityCommand = command as? CapabilityCommand ?: return null
        val capabilityKind = CapabilityKind.fromWireName(capabilityCommand.capability) ?: return null
        return ActionPreparationResult.Success(
            PreparedAction.Capability(
                GenericCapabilityRequest(
                    kind = capabilityKind,
                    operation = capabilityCommand.operation,
                    arguments = capabilityCommand.arguments,
                ),
            ),
        )
    }
}
