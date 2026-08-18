package com.carbroz.partner.sdui.engine.assembly

import com.carbroz.partner.sdui.engine.result.SduiParseResult
import com.carbroz.partner.sdui.engine.validation.SduiValidationPolicy

public interface SduiScreenAssembler {
    public fun assemble(
        jsonString: String,
        policy: SduiValidationPolicy = SduiValidationPolicy()
    ): SduiParseResult
}
