package io.customer.shared.sdk.config

import io.ktor.util.*

/**
 * Adds module configurations to config map. This can be called from module initializer to register
 * module configurations before they are accessed by other classes.
 */
fun <C : ModuleConfig> CustomerIOConfig.addModuleConfig(config: C) {
    moduleConfig[config.key] = config
}

/**
 * Gets module configurations from config map. If the configuration is not found in the map, calls
 * the defaultValue function and returns it without adding it to the map.
 */
fun <C : ModuleConfig> CustomerIOConfig.getModuleConfig(key: String, defaultValue: () -> C): C {
    @Suppress("UNCHECKED_CAST")
    return moduleConfig[key] as? C ?: defaultValue()
}

/**
 * Extension method to get [TrackingConfig] conveniently.
 *
 * Returns configurations set by user; fallbacks to default value if not set.
 */
internal val CustomerIOConfig.trackingConfig: TrackingConfig
    get() = getModuleConfig(key = TrackingConfig.NAME) { TrackingConfig.default() }
