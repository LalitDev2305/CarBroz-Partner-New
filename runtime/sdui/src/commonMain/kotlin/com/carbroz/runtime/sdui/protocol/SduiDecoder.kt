package com.carbroz.runtime.sdui.protocol

import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json

sealed interface SduiDecodeResult {
    data class Success(val envelope: SduiEnvelopeDto) : SduiDecodeResult
    data class Failure(val error: SduiDecodeError) : SduiDecodeResult
}

enum class SduiDecodeError {
    EmptyPayload,
    PayloadTooLarge,
    MalformedPayload,
}

/** Decodes untrusted JSON into transport DTOs without exposing serialization exceptions. */
class SduiDecoder(
    private val limits: SduiProtocolLimits = SduiProtocolLimits(),
    private val json: Json = Json {
        ignoreUnknownKeys = false
        isLenient = false
        explicitNulls = false
        coerceInputValues = false
    },
) {
    fun decode(payload: String): SduiDecodeResult {
        if (payload.isBlank()) return SduiDecodeResult.Failure(SduiDecodeError.EmptyPayload)
        if (payload.length > limits.maxPayloadCharacters) {
            return SduiDecodeResult.Failure(SduiDecodeError.PayloadTooLarge)
        }

        return try {
            SduiDecodeResult.Success(json.decodeFromString<SduiEnvelopeDto>(payload))
        } catch (_: SerializationException) {
            SduiDecodeResult.Failure(SduiDecodeError.MalformedPayload)
        } catch (_: IllegalArgumentException) {
            SduiDecodeResult.Failure(SduiDecodeError.MalformedPayload)
        }
    }
}
