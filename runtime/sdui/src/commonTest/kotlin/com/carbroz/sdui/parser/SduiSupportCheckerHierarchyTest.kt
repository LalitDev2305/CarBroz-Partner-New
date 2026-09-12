package com.carbroz.sdui.parser

import com.carbroz.sdui.model.SduiAction
import com.carbroz.sdui.model.SduiAuthentication
import com.carbroz.sdui.model.SduiComponent
import com.carbroz.sdui.model.SduiDestination
import com.carbroz.sdui.model.SduiElement
import com.carbroz.sdui.model.SduiGroup
import com.carbroz.sdui.model.SduiNavigationMode
import com.carbroz.sdui.model.SduiRequestMethod
import com.carbroz.sdui.model.SduiScreen
import com.carbroz.sdui.model.SduiSection
import com.carbroz.sdui.model.SduiTargetApp
import com.carbroz.sdui.model.SduiTemplate
import com.carbroz.sdui.registry.SduiNodeRegistration
import kotlinx.serialization.json.JsonObject
import kotlin.test.Test
import kotlin.test.assertEquals

class SduiSupportCheckerHierarchyTest {
    private val checker = SduiSupportChecker(SduiNodeRegistration.createRegistry())

    @Test
    fun unsupportedComponentIsRejected() {
        val base = screen()
        val result = checker.check(
            base.copy(
                template = base.template.copy(
                    components = listOf(base.template.components.single().copy(type = "unknown_component")),
                ),
            ),
        )

        assertEquals(SduiSupportResult.Unsupported("unsupported_component:unknown_component"), result)
    }

    @Test
    fun unsupportedSectionIsRejected() {
        val base = screen()
        val component = base.template.components.single().copy(
            elements = null,
            sections = listOf(
                SduiSection(
                    id = "section",
                    type = "unknown_section",
                    elements = listOf(textElement()),
                ),
            ),
        )

        val result = checker.check(base.copy(template = base.template.copy(components = listOf(component))))

        assertEquals(SduiSupportResult.Unsupported("unsupported_section:unknown_section"), result)
    }

    @Test
    fun unsupportedGroupIsRejected() {
        val base = screen()
        val component = base.template.components.single().copy(
            elements = null,
            sections = listOf(
                SduiSection(
                    id = "section",
                    type = "stack_section",
                    groups = listOf(
                        SduiGroup(
                            id = "group",
                            type = "unknown_group",
                            elements = listOf(textElement()),
                        ),
                    ),
                ),
            ),
        )

        val result = checker.check(base.copy(template = base.template.copy(components = listOf(component))))

        assertEquals(SduiSupportResult.Unsupported("unsupported_group:unknown_group"), result)
    }

    @Test
    fun unsafeNavigateEndpointIsRejectedBeforeExecution() {
        val result = checker.check(screenWithNavigate(endpoint = "https://example.com/screens/next"))

        assertEquals(SduiSupportResult.Unsupported("unsafe_endpoint"), result)
    }

    @Test
    fun nonGetNavigateDestinationIsRejectedBeforeExecution() {
        val result = checker.check(screenWithNavigate(method = SduiRequestMethod.POST))

        assertEquals(
            SduiSupportResult.Unsupported("unsupported_destination_method:POST"),
            result,
        )
    }

    private fun screenWithNavigate(
        endpoint: String = "/api/v1/screens/next",
        method: SduiRequestMethod = SduiRequestMethod.GET,
    ): SduiScreen {
        val base = screen()
        val navigate = SduiAction.Navigate(
            payload = SduiDestination(
                screenId = "next",
                templateId = "next_template",
                templateType = "stack_template",
                endpoint = endpoint,
                method = method,
                authentication = SduiAuthentication.NONE,
            ),
            navigationMode = SduiNavigationMode.PUSH,
        )
        val element = textElement().copy(actions = mapOf("onClick" to navigate))
        val component = base.template.components.single().copy(elements = listOf(element))
        return base.copy(template = base.template.copy(components = listOf(component)))
    }

    private fun screen(): SduiScreen = SduiScreen(
        screenId = "support_test",
        schemaVersion = "3.0",
        targetApp = SduiTargetApp.PARTNER,
        template = SduiTemplate(
            id = "support_template",
            type = "stack_template",
            components = listOf(
                SduiComponent(
                    id = "root",
                    type = "stack_component",
                    elements = listOf(textElement()),
                ),
            ),
        ),
    )

    private fun textElement(): SduiElement = SduiElement(
        id = "text",
        type = "text",
        properties = JsonObject(emptyMap()),
    )
}
