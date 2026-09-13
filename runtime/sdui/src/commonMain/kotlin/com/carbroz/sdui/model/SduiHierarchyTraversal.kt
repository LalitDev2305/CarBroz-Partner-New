package com.carbroz.sdui.model

/**
 * Canonical non-render traversal of the frozen additive hierarchy.
 *
 * Rendering keeps its nested composable traversal so parent layout ownership remains explicit;
 * support checks, field discovery and target lookup use this walker to avoid hierarchy drift.
 */
fun SduiTemplate.walk(
    onComponent: (SduiComponent) -> Unit = {},
    onSection: (SduiSection) -> Unit = {},
    onGroup: (SduiGroup) -> Unit = {},
    onElement: (SduiElement) -> Unit = {},
) {
    components.forEach { component ->
        onComponent(component)
        component.elements.orEmpty().forEach(onElement)
        component.sections.orEmpty().forEach { section ->
            onSection(section)
            section.elements.orEmpty().forEach(onElement)
            section.groups.orEmpty().forEach { group ->
                onGroup(group)
                group.elements.forEach(onElement)
            }
        }
    }
}

fun SduiTemplate.elementsInOrder(): List<SduiElement> = buildList {
    walk(onElement = { add(it) })
}

fun SduiTemplate.findElement(id: String): SduiElement? {
    var result: SduiElement? = null
    walk(onElement = { element ->
        if (result == null && element.id == id) result = element
    })
    return result
}
