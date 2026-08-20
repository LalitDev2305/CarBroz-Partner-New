package com.carbroz.partner.core.ui.adaptive.result

import androidx.compose.runtime.Immutable

@Immutable
sealed interface ResolutionResult<out T> {
    data class Resolved<out T>(val value: T) : ResolutionResult<T>
    data class Unsupported(val tokenKey: String) : ResolutionResult<Nothing>
}
