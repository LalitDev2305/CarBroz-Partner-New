package com.carbroz.partner.composition

import com.carbroz.runtime.sdui.model.NodeType
import com.carbroz.runtime.sdui.model.RequestAuthentication
import com.carbroz.runtime.sdui.model.RequestMethod
import com.carbroz.runtime.sdui.model.ScreenDestination
import com.carbroz.runtime.sdui.model.ScreenTransition
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.decodeFromJsonElement

@Serializable
data class DynamicScreenInstructionDto(
    val screenId: String,
    val templateId: String,
    val templateType: String,
    val endpoint: String,
    val method: String = "GET",
    val authentication: String = "SESSION",
    val transition: String = "PUSH",
    val backStackKey: String? = null,
    val restorePolicy: String = "CACHE_FIRST",
    val payload: JsonObject = JsonObject(emptyMap()),
)

sealed interface DynamicInstructionDecodeResult {
    data class Success(val instruction: DynamicScreenInstruction) : DynamicInstructionDecodeResult
    data class Failure(val code: String) : DynamicInstructionDecodeResult
}

/** Single transport/persistence codec for every backend-provided dynamic destination. */
class DynamicScreenInstructionCodec(
    private val json: Json = Json { ignoreUnknownKeys = true },
) {
    fun decode(element: JsonElement): DynamicInstructionDecodeResult {
        val dto = runCatching { json.decodeFromJsonElement<DynamicScreenInstructionDto>(element) }
            .getOrElse { return DynamicInstructionDecodeResult.Failure("dynamic_instruction_invalid_payload") }
        return normalize(dto)
    }

    fun decode(payload: String): DynamicInstructionDecodeResult {
        val dto = runCatching { json.decodeFromString<DynamicScreenInstructionDto>(payload) }
            .getOrElse { return DynamicInstructionDecodeResult.Failure("dynamic_instruction_invalid_payload") }
        return normalize(dto)
    }

    fun encode(instruction: DynamicScreenInstruction): String = json.encodeToString(
        DynamicScreenInstructionDto(
            screenId = instruction.destination.screenId,
            templateId = instruction.destination.templateId,
            templateType = instruction.destination.templateType.value,
            endpoint = instruction.request.endpoint,
            method = instruction.request.method.name,
            authentication = instruction.request.authentication.name,
            transition = instruction.transition.name,
            backStackKey = instruction.backStackKey,
            restorePolicy = instruction.restorePolicy.name,
            payload = instruction.request.payload,
        ),
    )

    private fun normalize(dto: DynamicScreenInstructionDto): DynamicInstructionDecodeResult {
        if (dto.screenId.isBlank() || dto.templateId.isBlank() || dto.templateType.isBlank()) {
            return DynamicInstructionDecodeResult.Failure("dynamic_instruction_invalid_identity")
        }
        if (!dto.endpoint.startsWith('/') || dto.endpoint.startsWith("//") || "://" in dto.endpoint) {
            return DynamicInstructionDecodeResult.Failure("dynamic_instruction_invalid_endpoint")
        }
        val method = enumValue<RequestMethod>(dto.method)
            ?: return DynamicInstructionDecodeResult.Failure("dynamic_instruction_invalid_method")
        val authentication = enumValue<RequestAuthentication>(dto.authentication)
            ?: return DynamicInstructionDecodeResult.Failure("dynamic_instruction_invalid_authentication")
        val transition = enumValue<ScreenTransition>(dto.transition)
            ?: return DynamicInstructionDecodeResult.Failure("dynamic_instruction_invalid_transition")
        val restorePolicy = enumValue<DynamicRestorePolicy>(dto.restorePolicy)
            ?: return DynamicInstructionDecodeResult.Failure("dynamic_instruction_invalid_restore_policy")

        return DynamicInstructionDecodeResult.Success(
            DynamicScreenInstruction(
                destination = ScreenDestination(dto.screenId, dto.templateId, NodeType(dto.templateType)),
                request = DynamicScreenRequest(method, dto.endpoint, dto.payload, authentication),
                transition = transition,
                backStackKey = dto.backStackKey?.takeIf { it.isNotBlank() } ?: dto.screenId,
                restorePolicy = restorePolicy,
            ),
        )
    }

    private inline fun <reified T : Enum<T>> enumValue(value: String): T? =
        enumValues<T>().firstOrNull { it.name == value.uppercase() }
}
