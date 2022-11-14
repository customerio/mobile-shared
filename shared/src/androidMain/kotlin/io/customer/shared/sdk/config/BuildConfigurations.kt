package io.customer.shared.sdk.config

import io.customer.shared.BuildConfig

actual fun getBuildConfigurations(): BuildConfigurations = AndroidBuildConfigurations()

/**
 * Build configurations class to hold Android Platform settings. This can be used as bridge to
 * pass values from auto generated BuildConfig to KMM.
 */
class AndroidBuildConfigurations : BuildConfigurations {
    override val isDebuggable: Boolean = BuildConfig.DEBUG
}
