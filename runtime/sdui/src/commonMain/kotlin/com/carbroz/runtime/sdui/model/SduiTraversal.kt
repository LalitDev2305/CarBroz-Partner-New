package com.carbroz.runtime.sdui.model

/** Canonical typed traversal for the flexible hierarchy; callers never reimplement branch walking. */
fun Screen.elements(): Sequence<Element> = sequence {
    template.components.forEach { component ->
        when (val content = component.content) {
            is ComponentContent.Elements -> yieldAll(content.values)
            is ComponentContent.Sections -> content.values.forEach { section ->
                when (val sectionContent = section.content) {
                    is SectionContent.Elements -> yieldAll(sectionContent.values)
                    is SectionContent.Groups -> sectionContent.values.forEach { group ->
                        yieldAll(group.elements)
                    }
                }
            }
        }
    }
}

fun Screen.nodes(): Sequence<Pair<NodeKind, NodePath>> = sequence {
    yield(NodeKind.TEMPLATE to template.path)
    template.components.forEach { component ->
        yield(NodeKind.COMPONENT to component.path)
        when (val content = component.content) {
            is ComponentContent.Elements -> content.values.forEach { yield(NodeKind.ELEMENT to it.path) }
            is ComponentContent.Sections -> content.values.forEach { section ->
                yield(NodeKind.SECTION to section.path)
                when (val sectionContent = section.content) {
                    is SectionContent.Elements -> sectionContent.values.forEach { yield(NodeKind.ELEMENT to it.path) }
                    is SectionContent.Groups -> sectionContent.values.forEach { group ->
                        yield(NodeKind.GROUP to group.path)
                        group.elements.forEach { yield(NodeKind.ELEMENT to it.path) }
                    }
                }
            }
        }
    }
}
