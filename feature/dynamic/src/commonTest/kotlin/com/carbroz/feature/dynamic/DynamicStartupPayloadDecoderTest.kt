package com.carbroz.feature.dynamic

import com.carbroz.runtime.application.bootstrap.StartupPayloadDecodeResult
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class DynamicStartupPayloadDecoderTest {
    private val decoder = DynamicStartupPayloadDecoder(DynamicScreenInstructionCodec())

    @Test
    fun `valid serialized next screen becomes trusted dynamic instruction`() {
        val result = assertIs<StartupPayloadDecodeResult.Success>(decoder.decode(payload()))
        assertIs<DynamicScreenInstruction>(result.payload)
    }

    @Test
    fun `invalid identity fails closed`() {
        assertFailure(payload(screenId = ""), "dynamic_instruction_invalid_identity")
    }

    @Test
    fun `absolute endpoint fails closed`() {
        assertFailure(
            payload(endpoint = "https://evil.example/login"),
            "dynamic_instruction_invalid_endpoint",
        )
    }

    @Test
    fun `invalid method fails closed`() {
        assertFailure(payload(method = "TRACE"), "dynamic_instruction_invalid_method")
    }

    @Test
    fun `invalid authentication fails closed`() {
        assertFailure(payload(authentication = "MAGIC"), "dynamic_instruction_invalid_authentication")
    }

    @Test
    fun `invalid transition fails closed`() {
        assertFailure(payload(transition = "MAGIC"), "dynamic_instruction_invalid_transition")
    }

    @Test
    fun `invalid restore policy fails closed`() {
        assertFailure(payload(restorePolicy = "MAGIC"), "dynamic_instruction_invalid_restore_policy")
    }

    private fun assertFailure(payload: String, expectedCode: String) {
        val result = assertIs<StartupPayloadDecodeResult.Failure>(decoder.decode(payload))
        assertEquals(expectedCode, result.code)
    }

    private fun payload(
        screenId: String = "partner_login",
        endpoint: String = "/api/v1/partner/sdui/registry/partner_login",
        method: String = "GET",
        authentication: String = "NONE",
        transition: String = "RESET",
        restorePolicy: String = "CACHE_FIRST",
    ): String = buildJsonObject {
        put("screenId", screenId)
        put("templateId", "partner_login_template")
        put("templateType", "form_template")
        put("endpoint", endpoint)
        put("method", method)
        put("authentication", authentication)
        put("transition", transition)
        put("restorePolicy", restorePolicy)
    }.toString()
}
