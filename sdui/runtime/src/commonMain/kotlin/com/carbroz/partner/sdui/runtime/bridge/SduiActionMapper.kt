package com.carbroz.partner.sdui.runtime.bridge

import com.carbroz.partner.engine.execution.action.ActionId
import com.carbroz.partner.engine.execution.action.ActionParameters
import com.carbroz.partner.engine.execution.action.ActionSpec
import com.carbroz.partner.engine.execution.action.ActionType
import com.carbroz.partner.engine.execution.action.ActionValue
import com.carbroz.partner.engine.execution.binding.BindingExpression
import com.carbroz.partner.sdui.engine.model.SduiAction
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.longOrNull

public class SduiActionMapper {

    public fun mapToActionSpec(
        triggerNodeId: String,
        action: SduiAction
    ): ActionSpec {
        val paramMap = mutableMapOf<String, ActionValue>()
        paramMap["endpoint"] = ActionValue.Text(action.api)
        paramMap["auth_policy"] = ActionValue.Text(action.authPolicy)

        action.templateId?.let { paramMap["template_id"] = ActionValue.Text(it) }
        action.templateType?.let { paramMap["template_type"] = ActionValue.Text(it) }

        action.payload?.forEach { (key, element) ->
            paramMap[key] = mapJsonElement(element)
        }

        return ActionSpec.create(
            id = ActionId("act_$triggerNodeId"),
            type = ActionType.API_REQUEST,
            parameters = ActionParameters.create(paramMap)
        )
    }

    private fun mapJsonElement(element: JsonElement): ActionValue {
        if (element is JsonPrimitive) {
            if (element.isString) {
                val str = element.content
                if (str.startsWith("\${") && str.endsWith("}")) {
                    try {
                        return ActionValue.Binding(BindingExpression(str))
                    } catch (_: IllegalArgumentException) {
                        return ActionValue.Text(str)
                    }
                }
                return ActionValue.Text(str)
            }
            element.longOrNull?.let { return ActionValue.Integer(it) }
            element.booleanOrNull?.let { return ActionValue.Flag(it) }
            return ActionValue.Text(element.content)
        }
        return ActionValue.Text(element.toString())
    }
}
