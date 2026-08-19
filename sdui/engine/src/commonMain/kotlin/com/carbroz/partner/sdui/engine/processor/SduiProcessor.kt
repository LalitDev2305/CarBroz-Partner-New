package com.carbroz.partner.sdui.engine.processor

import com.carbroz.partner.core.observability.logger.BoundLogger
import com.carbroz.partner.sdui.engine.assembly.DefaultSduiScreenAssembler
import com.carbroz.partner.sdui.engine.assembly.SduiScreenAssembler
import com.carbroz.partner.sdui.engine.mapper.DefaultSduiMapper
import com.carbroz.partner.sdui.engine.mapper.SduiMapper
import com.carbroz.partner.sdui.engine.parser.DefaultSduiParser
import com.carbroz.partner.sdui.engine.parser.SduiParser
import com.carbroz.partner.sdui.engine.result.SduiParseError
import com.carbroz.partner.sdui.engine.result.SduiParseResult
import com.carbroz.partner.sdui.engine.validation.DefaultSduiValidator
import com.carbroz.partner.sdui.engine.validation.SduiValidationPolicy
import com.carbroz.partner.sdui.engine.validation.SduiValidator

public class SduiProcessor(
    private val parser: SduiParser = DefaultSduiParser(),
    private val validator: SduiValidator = DefaultSduiValidator(),
    private val mapper: SduiMapper = DefaultSduiMapper(),
    private val assembler: SduiScreenAssembler = DefaultSduiScreenAssembler(parser, validator, mapper),
    private val logger: BoundLogger? = null
) {
    public fun process(
        jsonString: String,
        policy: SduiValidationPolicy = SduiValidationPolicy()
    ): SduiParseResult {
        return assembler.assemble(jsonString, policy)
    }
}
