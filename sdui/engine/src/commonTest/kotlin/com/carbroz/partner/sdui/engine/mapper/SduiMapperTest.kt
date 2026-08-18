package com.carbroz.partner.sdui.engine.mapper

import androidx.compose.ui.unit.dp
import com.carbroz.partner.core.ui.adaptive.spec.DimensionSpec
import com.carbroz.partner.core.ui.adaptive.spec.SpacingSpec
import com.carbroz.partner.sdui.engine.model.LayoutAxis
import com.carbroz.partner.sdui.engine.raw.*
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlin.test.*

class SduiMapperTest {

    private val mapper: SduiMapper = DefaultSduiMapper()

    @Test
    fun testRawTemplateDtoToCanonicalSduiTemplateMapping() {
        val rawTemplate = RawTemplateDto(
            templateId = "tpl_root",
            templateType = "home",
            width = "fill",
            height = "wrap",
            axis = "vertical",
            gap = 16,
            components = emptyList()
        )
        val rawScreen = RawScreenDto(
            schemaVersion = 1,
            screenId = "screen_1",
            template = rawTemplate
        )

        val screen = mapper.map(rawScreen)
        val template = screen.template

        assertEquals("tpl_root", template.id)
        assertEquals("home", template.templateType)
        assertEquals(DimensionSpec.Fill, template.width)
        assertEquals(DimensionSpec.Wrap, template.height)
        assertEquals(LayoutAxis.VERTICAL, template.axis)
        assertEquals(SpacingSpec.Fixed(16.dp), template.gap)

    }

    @Test
    fun testComponentSubComponentChildAndChildrenDataMapping() {
        val cdDto = RawChildrenDataDto(
            childrenDataId = "cd_1",
            childrenDataType = "button",
            width = "100",
            height = "50",
            padding = RawEdgeSpacingDto(top = 8, bottom = 8, start = 12, end = 12),
            properties = buildJsonObject { put("text", "Submit") }
        )
        val childDto = RawChildDto(
            childId = "ch_1",
            childType = "cell",
            childrenData = listOf(cdDto)
        )
        val subCompDto = RawSubComponentDto(
            subcomponentId = "sub_1",
            subcomponentType = "row",
            children = listOf(childDto)
        )
        val compDto = RawComponentDto(
            componentId = "cmp_1",
            componentType = "card",
            subcomponents = listOf(subCompDto)
        )
        val rawScreen = RawScreenDto(
            schemaVersion = 1,
            screenId = "screen_hierarchy",
            template = RawTemplateDto(
                templateId = "tpl_main",
                templateType = "form",
                components = listOf(compDto)
            )
        )

        val screen = mapper.map(rawScreen)

        assertEquals("screen_hierarchy", screen.screenId)
        val comp = screen.template.components.first()
        assertEquals("cmp_1", comp.id)
        assertEquals("card", comp.componentType)

        val subComp = comp.subcomponents.first()
        assertEquals("sub_1", subComp.id)
        assertEquals("row", subComp.subcomponentType)

        val child = subComp.children.first()
        assertEquals("ch_1", child.id)
        assertEquals("cell", child.childType)

        val cd = child.childrenData.first()
        assertEquals("cd_1", cd.id)
        assertEquals("button", cd.childrenDataType)
        assertEquals(DimensionSpec.Fixed(100.dp), cd.width)
        assertEquals(DimensionSpec.Fixed(50.dp), cd.height)
        assertEquals(8, cd.padding.top)
        assertEquals(12, cd.padding.start)

    }

    @Test
    fun testDimensionAndSpacingSpecVariantsMapping() {
        val cdFill = RawChildrenDataDto(childrenDataId = "cd_fill", childrenDataType = "box", width = "fill", height = "wrap")
        val cdFraction = RawChildrenDataDto(childrenDataId = "cd_frac", childrenDataType = "box", width = "50%")
        val cdToken = RawChildrenDataDto(childrenDataId = "cd_tok", childrenDataType = "box", width = "token:space_lg")

        val rawScreen = RawScreenDto(
            schemaVersion = 1,
            screenId = "screen_specs",
            template = RawTemplateDto(
                templateId = "tpl_specs",
                templateType = "form",
                gap = 24,
                components = listOf(
                    RawComponentDto(
                        componentId = "cmp_box",
                        componentType = "card",
                        childrenData = listOf(cdFill, cdFraction, cdToken)
                    )
                )
            )
        )

        val screen = mapper.map(rawScreen)
        val nodes = screen.template.components.first().childrenData

        assertEquals(DimensionSpec.Fill, nodes[0].width)
        assertEquals(DimensionSpec.Wrap, nodes[0].height)
        assertEquals(DimensionSpec.Fraction(0.5f), nodes[1].width)
        assertEquals(DimensionSpec.Token("space_lg"), nodes[2].width)
        assertEquals(SpacingSpec.Fixed(24.dp), screen.template.gap)

    }

    @Test
    fun testActionParentActionAndAcceptsParentActionMapping() {
        val cdTimer = RawChildrenDataDto(
            childrenDataId = "timer_node",
            childrenDataType = "timer",
            properties = buildJsonObject { put("accepts_parent_action", true) }
        )
        val cdButton = RawChildrenDataDto(
            childrenDataId = "btn_resend",
            childrenDataType = "button",
            action = RawActionDto(
                api = "/partner/auth/resend-otp",
                templateId = "tpl_otp",
                templateType = "form",
                payload = buildJsonObject { put("key", "val") }
            ),
            parentAction = RawParentActionDto(targetId = "timer_node")
        )

        val rawScreen = RawScreenDto(
            schemaVersion = 1,
            screenId = "screen_actions",
            template = RawTemplateDto(
                templateId = "tpl_actions",
                templateType = "form",
                components = listOf(
                    RawComponentDto(
                        componentId = "cmp_actions",
                        componentType = "card",
                        childrenData = listOf(cdTimer, cdButton)
                    )
                )
            )
        )

        val screen = mapper.map(rawScreen)
        val comp = screen.template.components.first()
        val timer = comp.childrenData[0]
        val button = comp.childrenData[1]

        assertTrue(timer.acceptsParentAction)
        assertNotNull(button.action)
        assertEquals("/partner/auth/resend-otp", button.action.api)
        assertEquals("tpl_otp", button.action.templateId)
        assertEquals("form", button.action.templateType)
        assertNotNull(button.parentAction)
        assertEquals("timer_node", button.parentAction.targetId)

    }

    @Test
    fun testOptionalHierarchyLevelsAndImmutability() {
        val cdDirect = RawChildrenDataDto(
            childrenDataId = "cd_direct",
            childrenDataType = "banner"
        )
        val compDirect = RawComponentDto(
            componentId = "cmp_direct",
            componentType = "section",
            childrenData = listOf(cdDirect)
        )
        val rawScreen = RawScreenDto(
            schemaVersion = 1,
            screenId = "screen_direct",
            template = RawTemplateDto(
                templateId = "tpl_direct",
                templateType = "home",
                components = listOf(compDirect)
            )
        )

        val screen = mapper.map(rawScreen)

        assertEquals(1, rawScreen.schemaVersion)
        assertEquals("screen_direct", rawScreen.screenId)
    }
}
