package com.carbroz.partner.sdui.runtime.bridge

import com.carbroz.partner.domain.actions.value.ActionValue
import com.carbroz.partner.sdui.engine.model.SduiAction
import kotlin.test.Test
import kotlin.test.assertEquals

class SduiActionMapperTest {

    private val mapper = SduiActionMapper()

    @Test
    fun testAuthPolicyPropagatedToActionParameters() {
        val actionNone = SduiAction(api = "/login", templateId = null, templateType = null, authPolicy = "none", payload = null)
        val actionReq = SduiAction(api = "/checkout", templateId = null, templateType = null, authPolicy = "required", payload = null)
        val actionOpt = SduiAction(api = "/feed", templateId = null, templateType = null, authPolicy = "optional", payload = null)

        val specNone = mapper.mapToActionSpec("node1", actionNone)
        val specReq = mapper.mapToActionSpec("node2", actionReq)
        val specOpt = mapper.mapToActionSpec("node3", actionOpt)

        assertEquals("none", (specNone.parameters.get("auth_policy") as? ActionValue.Text)?.value)
        assertEquals("required", (specReq.parameters.get("auth_policy") as? ActionValue.Text)?.value)
        assertEquals("optional", (specOpt.parameters.get("auth_policy") as? ActionValue.Text)?.value)
    }
}
