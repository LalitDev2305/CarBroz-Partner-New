package com.carbroz.partner.composition

import com.carbroz.foundation.configuration.AppConfiguration

@Deprecated("Use DynamicSduiRuntime", ReplaceWith("DynamicSduiRuntime"))
typealias ReferenceSduiRuntime = DynamicSduiRuntime

@Deprecated("Use createDynamicSduiRuntime(configuration)", ReplaceWith("createDynamicSduiRuntime(configuration)"))
internal fun createReferenceSduiRuntime(configuration: AppConfiguration): DynamicSduiRuntime =
    createDynamicSduiRuntime(configuration)
