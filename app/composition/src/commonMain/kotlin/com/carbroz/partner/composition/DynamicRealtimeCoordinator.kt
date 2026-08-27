package com.carbroz.partner.composition

import com.carbroz.data.realtime.RealtimeMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.decodeFromJsonElement

@Serializable
private data class DynamicRealtimeEnvelope(
    val type: String,
    val data: JsonObject = JsonObject(emptyMap()),
    val nextScreen: JsonElement? = null,
)

sealed interface DynamicRealtimeEvent {
    data class Data(val values: JsonObject) : DynamicRealtimeEvent
    data object RefreshCurrent : DynamicRealtimeEvent
    data class Navigate(val instruction: DynamicScreenInstruction) : DynamicRealtimeEvent
}

class DynamicRealtimeEventDecoder(
    private val instructionCodec: DynamicScreenInstructionCodec = DynamicScreenInstructionCodec(),
    private val json: Json = Json { ignoreUnknownKeys = true },
) {
    fun decode(message: RealtimeMessage): DynamicRealtimeEvent? {
        val root = runCatching { json.parseToJsonElement(message.payload) }.getOrNull() ?: return null
        val envelope = runCatching { json.decodeFromJsonElement<DynamicRealtimeEnvelope>(root) }.getOrNull() ?: return null
        return when (envelope.type.uppercase()) {
            "DATA" -> DynamicRealtimeEvent.Data(envelope.data)
            "REFRESH" -> DynamicRealtimeEvent.RefreshCurrent
            "SCREEN" -> {
                val next = envelope.nextScreen ?: return null
                when (val decoded = instructionCodec.decode(next)) {
                    is DynamicInstructionDecodeResult.Success -> DynamicRealtimeEvent.Navigate(decoded.instruction)
                    is DynamicInstructionDecodeResult.Failure -> null
                }
            }
            else -> null
        }
    }
}

/** Connects an already-authorized realtime message flow to the generic screen runtime. */
class DynamicRealtimeCoordinator(
    private val decoder: DynamicRealtimeEventDecoder = DynamicRealtimeEventDecoder(),
) {
    fun bind(messages: Flow<RealtimeMessage>, store: DynamicSduiStore, scope: CoroutineScope): Job = scope.launch {
        messages.collect { message ->
            when (val event = decoder.decode(message)) {
                is DynamicRealtimeEvent.Data -> store.onExternalData(event.values)
                DynamicRealtimeEvent.RefreshCurrent -> store.refreshFromExternalEvent()
                is DynamicRealtimeEvent.Navigate -> store.onExternalInstruction(event.instruction)
                null -> Unit
            }
        }
    }
}
