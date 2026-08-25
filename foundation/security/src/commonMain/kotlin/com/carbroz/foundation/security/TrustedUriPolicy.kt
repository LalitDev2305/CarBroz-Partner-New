package com.carbroz.foundation.security

sealed interface TrustedUriDecision {
    data object Trusted : TrustedUriDecision
    data class Rejected(val reason: String) : TrustedUriDecision
}

/**
 * Platform-neutral policy for URI launches originating from dynamic/runtime content.
 *
 * Schemes are allow-listed. HTTP(S) hosts can additionally be restricted when [allowedHosts]
 * is non-empty. Platform launchers must evaluate this policy before invoking native APIs.
 */
class TrustedUriPolicy(
    allowedSchemes: Set<String> = setOf("https", "mailto", "tel"),
    allowedHosts: Set<String> = emptySet(),
) {
    private val schemes = allowedSchemes.map { it.lowercase() }.toSet()
    private val hosts = allowedHosts.map { it.lowercase() }.toSet()

    init {
        require(schemes.isNotEmpty()) { "At least one trusted URI scheme is required" }
        require(schemes.none { it.isBlank() }) { "Trusted URI schemes must not be blank" }
        require(hosts.none { it.isBlank() }) { "Trusted URI hosts must not be blank" }
    }

    fun evaluate(uri: String): TrustedUriDecision {
        val value = uri.trim()
        if (value.isEmpty()) return TrustedUriDecision.Rejected("URI must not be blank")
        if (value.any { it.isWhitespace() || it.code < 0x20 }) {
            return TrustedUriDecision.Rejected("URI contains whitespace or control characters")
        }

        val separator = value.indexOf(':')
        if (separator <= 0) return TrustedUriDecision.Rejected("URI scheme is missing")
        val scheme = value.substring(0, separator).lowercase()
        if (scheme !in schemes) return TrustedUriDecision.Rejected("URI scheme '$scheme' is not trusted")

        if (scheme == "http" || scheme == "https") {
            val prefix = "$scheme://"
            if (!value.startsWith(prefix, ignoreCase = true)) {
                return TrustedUriDecision.Rejected("HTTP URI authority is missing")
            }
            val authority = value.substring(prefix.length).substringBefore('/').substringBefore('?').substringBefore('#')
            if (authority.isBlank() || '@' in authority) {
                return TrustedUriDecision.Rejected("HTTP URI authority is invalid")
            }
            val host = authority.substringBefore(':').lowercase()
            if (hosts.isNotEmpty() && host !in hosts) {
                return TrustedUriDecision.Rejected("URI host '$host' is not trusted")
            }
        }

        return TrustedUriDecision.Trusted
    }
}
