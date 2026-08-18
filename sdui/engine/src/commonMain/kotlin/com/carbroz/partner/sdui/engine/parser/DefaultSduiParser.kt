package com.carbroz.partner.sdui.engine.parser

import com.carbroz.partner.sdui.engine.raw.RawScreenDto
import kotlinx.serialization.json.Json

public class DefaultSduiParser(
    private val json: Json = Json {
        ignoreUnknownKeys = true
        isLenient = false
    }
) : SduiParser {
    override fun parse(jsonString: String): RawScreenDto {
        return json.decodeFromString(RawScreenDto.serializer(), jsonString)
    }
}
