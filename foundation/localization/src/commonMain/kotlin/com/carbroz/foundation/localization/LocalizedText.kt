package com.carbroz.foundation.localization

/**
 * Product-neutral text source that can represent either already-resolved content
 * or a localization key with typed string arguments.
 *
 * SDUI/runtime layers may carry [Key] without depending on Compose resources;
 * UI/resource adapters resolve it at the final presentation boundary.
 */
sealed interface LocalizedText {
    data class Literal(val value: String) : LocalizedText

    data class Key(
        val key: String,
        val arguments: List<String> = emptyList(),
    ) : LocalizedText {
        init {
            require(key.matches(Regex("^[a-z][a-z0-9_.-]{1,127}$"))) {
                "Localization key must be 2-128 lowercase characters using letters, digits, dot, underscore, or dash."
            }
        }
    }
}

/** Resolves [LocalizedText] for a specific locale/resource implementation. */
fun interface TextResolver {
    fun resolve(text: LocalizedText): String
}

/** Formats decimal values according to the active application locale. */
fun interface NumberFormatter {
    fun format(value: Double): String
}

/** Formats monetary minor units without embedding product/business rules. */
fun interface CurrencyFormatter {
    fun formatMinorUnits(amountMinor: Long, currencyCode: String): String
}

/** Formats an epoch-millisecond instant for presentation. */
fun interface DateTimeFormatter {
    fun format(epochMilliseconds: Long, zoneId: String): String
}
