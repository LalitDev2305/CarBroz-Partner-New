package com.carbroz.partner.startup

import com.carbroz.feature.dynamic.DynamicScreenInstruction
import com.carbroz.feature.dynamic.DynamicScreenInstructionCodec
import com.carbroz.runtime.application.startup.StartupBlocker
import com.carbroz.runtime.application.startup.StartupNotice
import com.carbroz.runtime.application.startup.StartupResolution
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class PartnerBootstrapPolicyEvaluatorTest {
    private val evaluator = PartnerBootstrapPolicyEvaluator(DynamicScreenInstructionCodec())

    @Test
    fun `required update has precedence over maintenance and requires a store URL`() {
        val result = evaluator.evaluate(
            data(
                required = true,
                maintenance = true,
                storeUrl = "https://example.com/update",
            ),
        )

        val resolution = assertIs<PartnerBootstrapPolicyResult.Resolved>(result).resolution
        assertIs<StartupBlocker.RequiredUpdate>(assertIs<StartupResolution.Blocked>(resolution).blocker)
    }

    @Test
    fun `required update without URL fails closed`() {
        assertEquals(
            PartnerBootstrapPolicyResult.Invalid("bootstrap_required_update_missing_store_url"),
            evaluator.evaluate(data(required = true, storeUrl = null)),
        )
    }

    @Test
    fun `maintenance becomes retryable blocked outcome`() {
        val resolution = assertIs<PartnerBootstrapPolicyResult.Resolved>(
            evaluator.evaluate(data(maintenance = true)),
        ).resolution

        val blocker = assertIs<StartupBlocker.Maintenance>(
            assertIs<StartupResolution.Blocked>(resolution).blocker,
        )
        assertEquals(true, blocker.retryable)
    }

    @Test
    fun `optional update is non blocking notice and next screen remains startup payload`() {
        val resolution = assertIs<StartupResolution.Ready>(
            assertIs<PartnerBootstrapPolicyResult.Resolved>(
                evaluator.evaluate(
                    data(optional = true, storeUrl = "https://example.com/update", latestVersion = "1.1.0"),
                ),
            ).resolution,
        )

        assertIs<DynamicScreenInstruction>(resolution.payload)
        assertEquals(
            listOf(StartupNotice.OptionalUpdate("1.1.0", "https://example.com/update")),
            resolution.notices,
        )
    }

    @Test
    fun `unsafe absolute next screen endpoint fails closed`() {
        val result = evaluator.evaluate(
            data(
                nextScreen = nextScreen(endpoint = "https://evil.example/login"),
            ),
        )

        assertIs<PartnerBootstrapPolicyResult.Invalid>(result)
    }

    private fun data(
        required: Boolean = false,
        optional: Boolean = false,
        maintenance: Boolean = false,
        storeUrl: String? = null,
        latestVersion: String = "1.0.0",
        nextScreen: kotlinx.serialization.json.JsonElement = nextScreen(),
    ) = PartnerBootstrapData(
        config = PartnerBootstrapConfig(
            version = "1",
            maintenance = PartnerMaintenanceConfig(
                enabled = maintenance,
                title = "Maintenance",
                message = "Try later",
            ),
            update = PartnerUpdateConfig(
                required = required,
                optional = optional,
                minimumVersion = "1.0.0",
                latestVersion = latestVersion,
                storeUrl = storeUrl,
            ),
            features = PartnerFeatureConfig(
                registrationEnabled = true,
                individualPartnerEnabled = true,
                organizationPartnerEnabled = true,
            ),
        ),
        startup = PartnerBootstrapStartup(
            authenticated = false,
            nextScreen = nextScreen,
        ),
    )

    private fun nextScreen(
        endpoint: String = "/api/v1/partner/sdui/registry/partner_login",
    ) = buildJsonObject {
        put("screenId", "partner_login")
        put("templateId", "partner_login_template")
        put("templateType", "form_template")
        put("endpoint", endpoint)
        put("method", "GET")
        put("authentication", "NONE")
    }
}
