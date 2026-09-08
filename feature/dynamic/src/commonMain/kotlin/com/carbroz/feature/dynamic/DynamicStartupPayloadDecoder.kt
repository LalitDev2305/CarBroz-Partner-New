package com.carbroz.feature.dynamic

import com.carbroz.runtime.application.bootstrap.StartupPayloadDecodeResult
import com.carbroz.runtime.application.bootstrap.StartupPayloadDecoder

/** Adapts the generic application bootstrap handoff to the existing trusted dynamic-screen codec. */
class DynamicStartupPayloadDecoder(
    private val codec: DynamicScreenInstructionCodec,
) : StartupPayloadDecoder {
    override fun decode(payload: String): StartupPayloadDecodeResult = when (val result = codec.decode(payload)) {
        is DynamicInstructionDecodeResult.Success -> StartupPayloadDecodeResult.Success(result.instruction)
        is DynamicInstructionDecodeResult.Failure -> StartupPayloadDecodeResult.Failure(result.code)
    }
}
