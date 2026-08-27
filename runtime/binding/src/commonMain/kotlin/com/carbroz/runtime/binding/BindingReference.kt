package com.carbroz.runtime.binding

/** Closed namespaces that may supply dynamic values at execution time. */
enum class BindingNamespace(val wireName: String) {
    FORM("form"),
    SCREEN("screen"),
    SESSION("session"),
    CONFIG("config"),
    EVENT("event"),
    RESULT("result"),
    RUNTIME("runtime"),
}

data class BindingReference(
    val namespace: BindingNamespace,
    val path: List<String>,
) {
    init {
        require(path.isNotEmpty()) { "Binding path cannot be empty." }
        require(path.all(::isValidSegment)) { "Binding path contains an invalid segment." }
    }

    override fun toString(): String = buildString {
        append('$')
        append(namespace.wireName)
        path.forEach { segment -> append('.').append(segment) }
    }

    companion object {
        private const val MAX_REFERENCE_LENGTH = 256
        private const val MAX_SEGMENTS = 16

        fun parse(value: String): BindingReference? {
            if (value.length !in 3..MAX_REFERENCE_LENGTH || value.firstOrNull() != '$') return null
            val parts = value.drop(1).split('.')
            if (parts.size !in 2..(MAX_SEGMENTS + 1)) return null
            val namespace = BindingNamespace.entries.firstOrNull { it.wireName == parts.first() } ?: return null
            val path = parts.drop(1)
            if (path.any { !isValidSegment(it) }) return null
            return BindingReference(namespace, path)
        }

        private fun isValidSegment(value: String): Boolean =
            value.isNotEmpty() &&
                value.length <= 64 &&
                value.first().let { it == '_' || it.isLetter() } &&
                value.all { it == '_' || it == '-' || it.isLetterOrDigit() }
    }
}
