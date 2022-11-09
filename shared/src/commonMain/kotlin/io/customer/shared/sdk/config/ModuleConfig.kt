package io.customer.shared.sdk.config

/**
 * Tagged interface to support dynamic configurations for Customer.io modules.
 *
 * Child class must implement [Builder] to enforce builder pattern for constructing configurations.
 */
interface ModuleConfig {
    /**
     * Unique identifier for the module
     */
    val key: String

    /**
     * Basic interface to implement builder pattern for module configurations.
     *
     * @param Config generic type of configurations required by the module.
     */
    interface Builder<out Config : ModuleConfig> {
        fun build(): Config
    }
}
