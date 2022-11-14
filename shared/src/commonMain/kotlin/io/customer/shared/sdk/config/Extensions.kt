package io.customer.shared.sdk.config

import io.customer.shared.sdk.meta.Region
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

/**
 * Extension method to get tracking api URL conveniently.
 *
 * Returns trackingApiUrl provided in config; fallbacks to tracking url from [Region].
 */
internal val CustomerIOConfig.trackingApiHostname: String
    get() {
        // TODO: Fix this when we move out of devbox testing
        // return network.trackingApiUrl ?: workspace.region.trackingURL
        return "https://track-v2.devzilla.customerio.dev/"
    }

internal val CustomerIOConfig.basicAuthHeaderSting: String
    get() {
        val apiKey = workspace.apiKey
        val siteId = workspace.siteId
        val rawHeader = "$siteId:$apiKey"
        return "Basic ${rawHeader.encodeBase64()}"
    }
