package io.customer.shared.sdk.config

/**
 * Configurations class to hold static platform related settings
 */
interface BuildConfigurations {
    /**
     * Indicated whether the build has debugging enabled or not
     */
    val isDebuggable: Boolean
}

internal expect fun getBuildConfigurations(): BuildConfigurations
