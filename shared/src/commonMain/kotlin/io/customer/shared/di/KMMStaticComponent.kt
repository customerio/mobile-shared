package io.customer.shared.di

import io.customer.shared.sdk.config.CustomerIOConfig
import io.customer.shared.util.*
import io.customer.shared.work.CoroutineExecutor
import io.customer.shared.work.CoroutineExecutorImpl

/**
 * Static component dependency graph to satisfy independent dependencies from single place. All
 * other graphs should never redefine dependencies available here unless extremely necessary. The
 * structure of DIGraphs would something like:
 *
 * -> StaticGraph (Independent)
 * --> WorkspaceGraph (Depends on StaticGraph, Platform and SDK Configurations)
 *
 * The class should only contain dependencies matching the following criteria:
 * - dependencies that may be required without SDK initialization.
 * - dependencies that are lightweight and are not dependent on SDK initialization.
 */
@Suppress("MemberVisibilityCanBePrivate", "unused")
class KMMStaticComponent : DIGraph() {
    internal val dateTimeUtil: DateTimeUtil
        get() = getSingletonInstance { DateTimeUtilImpl() }

    internal val dispatcher: Dispatcher
        get() = getSingletonInstance { KMMDispatcher() }

    internal val jsonAdapter: JsonAdapter
        get() = getSingletonInstance { JsonAdapterImpl() }

    val logger: Logger
        get() = getSingletonInstance { ConsoleLogger() }

    internal val coroutineExecutor: CoroutineExecutor
        get() = getNewInstance {
            CoroutineExecutorImpl(
                logger = logger,
                dispatcher = dispatcher,
            )
        }
}

/**
 * Updates static component instances to apply changes from user configurations. This method can
 * be called multiple times with changes from last call being reflected.
 */
internal fun KMMStaticComponent.attachSDKConfig(config: CustomerIOConfig) {
    logger.logLevel = config.sdkLogLevel
}
