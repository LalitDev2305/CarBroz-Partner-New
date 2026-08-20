package com.carbroz.partner.core.navigation

/**
 * Immutable value descriptor representing a navigation route and primitive parameters.
 *
 * Enforces route non-blank validation and defensive copying of parameter maps.
 */
class NavDestination private constructor(
    val route: String,
    params: Map<String, String>
) {
    val params: Map<String, String> = params.toMap()

    init {
        require(route.isNotBlank()) { "NavDestination route must not be blank" }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is NavDestination) return false
        return route == other.route && params == other.params
    }

    override fun hashCode(): Int {
        var result = route.hashCode()
        result = 31 * result + params.hashCode()
        return result
    }

    override fun toString(): String {
        return "NavDestination(route='$route', paramCount=${params.size})"
    }

    companion object {
        fun create(
            route: String,
            params: Map<String, String> = emptyMap()
        ): NavDestination {
            return NavDestination(route, params)
        }
    }
}
