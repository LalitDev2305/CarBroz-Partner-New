package com.carbroz.partner.sdui.engine.validation

import com.carbroz.partner.sdui.engine.raw.RawScreenDto
import com.carbroz.partner.sdui.engine.result.SduiParseError

public interface SduiValidator {
    public fun validate(
        rawScreen: RawScreenDto,
        jsonLengthBytes: Int,
        policy: SduiValidationPolicy = SduiValidationPolicy()
    ): SduiParseError?
}
