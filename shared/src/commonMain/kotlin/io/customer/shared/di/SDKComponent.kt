package io.customer.shared.di

import io.customer.shared.Platform
import io.customer.shared.device.UserAgentStore
import io.customer.shared.sdk.config.CustomerIOConfig

/**
 * SDK DI component to satisfy dependencies from SDK wrappers. Any implementation expected to be
 * fulfilled by the caller SDKs should be placed here.
 */
interface SDKComponent {
    val platform: Platform
    val customerIOConfig: CustomerIOConfig
    val userAgentStore: UserAgentStore
}
