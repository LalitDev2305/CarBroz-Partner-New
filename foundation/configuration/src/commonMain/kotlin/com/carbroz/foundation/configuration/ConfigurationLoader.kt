package com.carbroz.foundation.configuration

/**
 * Loads client configuration through the mandatory validation boundary.
 *
 * Application bootstrap should depend on this type rather than consuming a raw
 * [ConfigurationProvider] directly. This prevents infrastructure from starting
 * with structurally unsafe environment/build configuration.
 */
class ConfigurationLoader(
    private val provider: ConfigurationProvider,
) {
    fun load(): ConfigurationLoadResult {
        val configuration = provider.get()
        return when (val validation = ConfigurationValidator.validate(configuration)) {
            ConfigurationValidationResult.Valid -> ConfigurationLoadResult.Loaded(configuration)
            is ConfigurationValidationResult.Invalid -> ConfigurationLoadResult.Rejected(validation.violations)
        }
    }
}

sealed interface ConfigurationLoadResult {
    data class Loaded(val configuration: AppConfiguration) : ConfigurationLoadResult
    data class Rejected(val violations: List<ConfigurationViolation>) : ConfigurationLoadResult
}
