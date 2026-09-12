package com.carbroz.sdui.registry

object SduiNodeRegistration {
    fun createRegistry(): SduiNodeRegistry = SduiNodeRegistry().apply {
        registerTemplates(TemplateDefinitions.all)
        registerComponents(ComponentDefinitions.all)
        registerSections(SectionDefinitions.all)
        registerGroups(GroupDefinitions.all)
        registerElements(ElementDefinitions.all)
    }
}
