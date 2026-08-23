package com.carbroz.foundation.localization

/** Logical text/layout direction used by adaptive presentation code. */
enum class TextDirection {
    LeftToRight,
    RightToLeft,
}

/**
 * Resolves direction for the active locale without leaking platform locale APIs.
 *
 * Implementations may use richer script-aware platform data where available.
 */
fun interface TextDirectionProvider {
    fun direction(locale: AppLocale): TextDirection
}
