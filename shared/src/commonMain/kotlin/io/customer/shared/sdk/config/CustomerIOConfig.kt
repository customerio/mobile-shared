package io.customer.shared.sdk.config

import io.customer.shared.sdk.meta.Workspace
import io.customer.shared.util.LogLevel

/**
 * Config class to hold SDK configurations in a single place.
 *
 * @property sdkLogLevel the level of logs to print.
 * @property workspace configurations required to setup the workspace.
 * @property network configurations to hold network settings.
 * @property backgroundQueue configurations to hold background queue settings.
 */
data class CustomerIOConfig constructor(
    val sdkLogLevel: LogLevel = DefaultValue.LOG_LEVEL,
    val workspace: Workspace,
    val backgroundQueue: BackgroundQueueConfig = BackgroundQueueConfig.default(),
) {
    constructor(workspace: Workspace) : this(
        sdkLogLevel = DefaultValue.LOG_LEVEL,
        workspace = workspace,
    )

    constructor(workspace: Workspace, backgroundQueue: BackgroundQueueConfig) : this(
        sdkLogLevel = DefaultValue.LOG_LEVEL,
        workspace = workspace,
        backgroundQueue = backgroundQueue,
    )

    /**
     * Default values make it easier to reuse when providing fallback values in wrapper SDKs or
     * auto initializing the SDK.
     */
    internal object DefaultValue {
        val LOG_LEVEL = LogLevel.ERROR
    }
}
