package com.carbroz.partner.core.observability.policy

import com.carbroz.partner.core.observability.model.LogCategory
import com.carbroz.partner.core.observability.model.LogLevel

/**
 * Immutable configuration controlling verbosity, category enablement, and payload logging policies.
 */
data class ObservabilityConfig(
    val isEnabled: Boolean = true,
    val minLevel: LogLevel = LogLevel.INFO,
    val enabledCategories: Set<LogCategory> = LogCategory.entries.toSet(),
    val allowDetailedDiagnostics: Boolean = false,
    val allowPayloadLogging: Boolean = false,
    val enablePerformanceTiming: Boolean = true
)
