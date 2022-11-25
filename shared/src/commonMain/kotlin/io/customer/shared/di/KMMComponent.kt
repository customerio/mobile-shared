package io.customer.shared.di

import io.customer.shared.database.DatabaseDriverFactory
import io.customer.shared.database.DatabaseHelper
import io.customer.shared.database.getDatabaseDriverFactory

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
    private val staticComponent: KMMStaticComponent = KMMStaticComponent(),
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

}
