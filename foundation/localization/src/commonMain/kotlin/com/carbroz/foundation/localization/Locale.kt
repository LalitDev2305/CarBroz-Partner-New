package com.carbroz.foundation.localization

/**
 * Platform-neutral locale identity using the core BCP-47 language/script/region components.
 *
 * Language is required. Script and region are optional. Variants/extensions are intentionally
 * not modeled here: platform/resource adapters may preserve richer tags when needed, while
 * shared runtime/domain code gets the stable components required for language, script and
 * regional presentation decisions.
 */
data class AppLocale(
    val language: String,
    val script: String? = null,
    val region: String? = null,
) {
    init {
        require(language.matches(LANGUAGE_PATTERN)) { "Language must contain 2-3 ASCII letters." }
        require(script == null || script.matches(SCRIPT_PATTERN)) { "Script must contain exactly 4 ASCII letters." }
        require(region == null || region.matches(REGION_PATTERN)) {
            "Region must contain 2 ASCII letters or 3 digits."
        }
    }

    val languageTag: String = buildString {
        append(language.lowercase())
        script?.let {
            append('-')
            append(it.lowercase().replaceFirstChar(Char::uppercaseChar))
        }
        region?.let {
            append('-')
            append(if (it.all(Char::isDigit)) it else it.uppercase())
        }
    }

    private companion object {
        val LANGUAGE_PATTERN = Regex("^[a-zA-Z]{2,3}$")
        val SCRIPT_PATTERN = Regex("^[a-zA-Z]{4}$")
        val REGION_PATTERN = Regex("^(?:[a-zA-Z]{2}|[0-9]{3})$")
    }
}

/** Supplies the currently selected application locale. */
fun interface LocaleProvider {
    fun current(): AppLocale
}
