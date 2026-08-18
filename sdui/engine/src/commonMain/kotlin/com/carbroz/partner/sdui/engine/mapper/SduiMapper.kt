package com.carbroz.partner.sdui.engine.mapper

import com.carbroz.partner.sdui.engine.model.SduiScreen
import com.carbroz.partner.sdui.engine.raw.RawScreenDto

public interface SduiMapper {
    public fun map(rawScreen: RawScreenDto): SduiScreen
}
