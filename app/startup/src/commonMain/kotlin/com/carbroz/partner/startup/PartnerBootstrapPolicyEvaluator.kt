package com.carbroz.partner.startup

import com.carbroz.feature.dynamic.DynamicInstructionDecodeResult
import com.carbroz.feature.dynamic.DynamicScreenInstructionCodec
import com.carbroz.runtime.application.startup.StartupBlocker
import com.carbroz.runtime.application.startup.StartupNotice
import com.carbroz.runtime.application.startup.StartupResolution

/** Purely converts trusted Partner bootstrap data into a semantic startup decision. */
class PartnerBootstrapPolicyEvaluator(
    private val instructionCodec: DynamicScreenInstructionCodec,
) {
    fun evaluate(data: PartnerBootstrapData): PartnerBootstrapPolicyResult {
        val update = data.config.update
        if (update.required) {
            val updateUri = update.storeUrl?.takeIf(String::isNotBlank)
                ?: return PartnerBootstrapPolicyResult.Invalid("bootstrap_required_update_missing_store_url")
            return PartnerBootstrapPolicyResult.Resolved(
                StartupResolution.Blocked(
                    StartupBlocker.RequiredUpdate(
                        title = "Update CarBroz Partner",
                        message = "A newer version of CarBroz Partner is required to continue.",
                        updateUri = updateUri,
                    ),
                ),
            )
        }

        val maintenance = data.config.maintenance
        if (maintenance.enabled) {
            return PartnerBootstrapPolicyResult.Resolved(
                StartupResolution.Blocked(
                    StartupBlocker.Maintenance(
                        title = maintenance.title,
                        message = maintenance.message,
                        retryable = true,
                    ),
                ),
            )
        }

        return when (val decoded = instructionCodec.decode(data.startup.nextScreen)) {
            is DynamicInstructionDecodeResult.Failure ->
                PartnerBootstrapPolicyResult.Invalid("bootstrap_${decoded.code}")

            is DynamicInstructionDecodeResult.Success -> {
                val notices = if (update.optional) {
                    listOf(
                        StartupNotice.OptionalUpdate(
                            latestVersion = update.latestVersion,
                            updateUri = update.storeUrl?.takeIf(String::isNotBlank),
                        ),
                    )
                } else {
                    emptyList()
                }
                PartnerBootstrapPolicyResult.Resolved(
                    StartupResolution.Ready(
                        payload = decoded.instruction,
                        notices = notices,
                    ),
                )
            }
        }
    }
}

sealed interface PartnerBootstrapPolicyResult {
    data class Resolved(val resolution: StartupResolution) : PartnerBootstrapPolicyResult
    data class Invalid(val code: String) : PartnerBootstrapPolicyResult {
        init { require(code.isNotBlank()) }
    }
}
