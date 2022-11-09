package io.customer.shared.sdk.config

import io.customer.shared.sdk.meta.Workspace
import io.customer.shared.util.LogLevel

/**
 * Config class to hold SDK configurations in a single place.
 *
 * @property logLevel the level of logs to print.
 * @property workspace configurations required to setup the workspace.
 * @property network configurations to hold network settings.
 * @property backgroundQueue configurations to hold background queue settings.
 * @property moduleConfig map to hold module configurations, having a map it makes it easier to
 * attach them later.
 */
data class CustomerIOConfig(
    val logLevel: LogLevel = DefaultValue.LOG_LEVEL,
    val workspace: Workspace,
    val network: NetworkConfig = NetworkConfig.default(),
    val backgroundQueue: BackgroundQueueConfig = BackgroundQueueConfig.default(),
    internal val moduleConfig: MutableMap<String, ModuleConfig> = mutableMapOf(),
) {
    /**
     * Constant class to hold default values.
     */
    internal object DefaultValue {
        val LOG_LEVEL = LogLevel.ERROR
    }
}