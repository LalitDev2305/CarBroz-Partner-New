package com.carbroz.partner.sdui.engine.parser

import com.carbroz.partner.sdui.engine.raw.RawScreenDto

public interface SduiParser {
    public fun parse(jsonString: String): RawScreenDto
}
