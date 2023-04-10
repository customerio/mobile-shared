package io.customer.shared.di

import io.customer.shared.database.*
import io.customer.shared.tracking.queue.BackgroundQueue
import io.customer.shared.tracking.queue.BackgroundQueueImpl
import io.customer.shared.util.JsonAdapter
import io.customer.shared.util.JsonAdapterImpl

/**
 * Workspace component dependency graph to satisfy workspace based dependencies from single place.
 *
 * The class should only contain dependencies matching the following criteria:
 * - dependencies that requires workspace information to be initialized.
 * - dependencies that should be reinitialized whenever workspace is switched.
 * - or any dependency that cannot fit in DIGraphs above this graph :)
 *
 * @see [KMMStaticComponent] for complete graph hierarchy.
 */
@Suppress("MemberVisibilityCanBePrivate", "unused")
class KMMComponent(
    internal val staticComponent: KMMStaticComponent = KMMStaticComponent(),
    val sdkComponent: SDKComponent,
) : DIGraph() {
    constructor(sdkComponent: SDKComponent) : this(
        staticComponent = KMMStaticComponent(),
        sdkComponent = sdkComponent,
    )

    init {
        staticComponent.attachSDKConfig(config = sdkComponent.customerIOConfig)
    }

    private val databaseDriverFactory: DatabaseDriverFactory
        get() = getSingletonInstance { getDatabaseDriverFactory(platform = sdkComponent.platform) }

    private val databaseHelper: DatabaseHelper
        get() = getSingletonInstance { DatabaseHelper(databaseDriverFactory = databaseDriverFactory) }

    internal val jsonAdapter: JsonAdapter
        get() = getSingletonInstance {
            JsonAdapterImpl(
                logger = staticComponent.logger,
                sdkComponent = sdkComponent,
            )
        }

    val backgroundQueue: BackgroundQueue
        get() = getSingletonInstance {
            BackgroundQueueImpl(
                logger = staticComponent.logger,
                dateTimeUtil = staticComponent.dateTimeUtil,
                workspace = sdkComponent.customerIOConfig.workspace,
                platform = sdkComponent.platform,
                jobExecutor = staticComponent.jobExecutor,
                dispatcher = staticComponent.dispatcher,
                trackingTaskQueryHelper = trackingTaskQueryHelper,
            )
        }

    internal val trackingTaskQueryHelper: TrackingTaskQueryHelper
        get() = getSingletonInstance {
            TrackingTaskQueryHelperImpl(
                logger = staticComponent.logger,
                dateTimeUtil = staticComponent.dateTimeUtil,
                jsonAdapter = jsonAdapter,
                databaseUtil = staticComponent.databaseUtil,
                workspace = sdkComponent.customerIOConfig.workspace,
                backgroundQueueConfig = sdkComponent.customerIOConfig.backgroundQueue,
                trackingTaskDAO = databaseHelper.trackingTaskDAO,
            )
        }

}
