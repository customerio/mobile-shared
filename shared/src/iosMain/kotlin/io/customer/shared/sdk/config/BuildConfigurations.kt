package io.customer.shared.sdk.config

actual fun getBuildConfigurations(): BuildConfigurations = IOSBuildConfigurations()

/**
 * Build configurations class to hold iOS Platform settings
 */
class IOSBuildConfigurations : BuildConfigurations {
    // TODO: Pass the correct value
    override val isDebuggable: Boolean = true
}
