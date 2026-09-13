package com.carbroz.sdui.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertSame

class SduiHierarchyTraversalTest {
    @Test
    fun walkPreservesBothAdditiveBranchesInFrozenOrder() {
        val directComponentElement = element("component_element")
        val directSectionElement = element("section_element")
        val groupElement = element("group_element")
        val group = SduiGroup(id = "group", type = "stack_group", elements = listOf(groupElement))
        val section = SduiSection(
            id = "section",
            type = "stack_section",
            elements = listOf(directSectionElement),
            groups = listOf(group),
        )
        val component = SduiComponent(
            id = "component",
            type = "stack_component",
            elements = listOf(directComponentElement),
            sections = listOf(section),
        )
        val template = SduiTemplate(
            id = "template",
            type = "stack_template",
            components = listOf(component),
        )

        val visited = mutableListOf<String>()
        template.walk(
            onComponent = { visited += it.id },
            onSection = { visited += it.id },
            onGroup = { visited += it.id },
            onElement = { visited += it.id },
        )

        assertEquals(
            listOf("component", "component_element", "section", "section_element", "group", "group_element"),
            visited,
        )
        assertEquals(listOf(directComponentElement, directSectionElement, groupElement), template.elementsInOrder())
        assertSame(groupElement, template.findElement("group_element"))
    }

    private fun element(id: String): SduiElement = SduiElement(id = id, type = "text")
}
