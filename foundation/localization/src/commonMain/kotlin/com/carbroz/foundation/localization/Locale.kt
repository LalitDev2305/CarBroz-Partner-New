package com.carbroz.foundation.localization

/**
 * Platform-neutral locale identity using normalized BCP-47-like components.
 *
 * Language is required; region is optional. UI/resource adapters may translate
 * this model to their platform locale type without leaking that type into domain
 * or runtime code.
 */
data class AppLocale(
    val language: String,
    val region: String? = null,
) {
    init {
        require(language.matches(Regex("^[a-zA-Z]{2,3}$"))) { "Language must contain 2-3 ASCII letters." }
        require(region == null || region.matches(Regex("^(?:[a-zA-Z]{2}|[0-9]{3})$"))) {
            "Region must contain 2 ASCII letters or 3 digits."
        }
    }

    val languageTag: String = buildString {
        append(language.lowercase())
        region?.let {
            append('-')
            append(if (it.all(Char::isDigit)) it else it.uppercase())
        }
    }
}

/** Supplies the currently selected application locale. */
fun interface LocaleProvider {
    fun current(): AppLocale
}
