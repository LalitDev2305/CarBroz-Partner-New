package com.carbroz.partner.sdui.runtime.bridge

import com.carbroz.partner.domain.actions.value.ActionValue
import com.carbroz.partner.engine.execution.binding.BindingScope

public class SduiBindingScopeAdapter(
    private val nodeValues: Map<String, String>
) : BindingScope {

    override fun resolveValue(path: String): ActionValue? {
        val trimmed = path.trim()
        val key = if (trimmed.startsWith("form.")) {
            trimmed.removePrefix("form.")
        } else {
            trimmed
        }
        val rawValue = nodeValues[key] ?: return null
        return ActionValue.Text(rawValue)
    }
}
