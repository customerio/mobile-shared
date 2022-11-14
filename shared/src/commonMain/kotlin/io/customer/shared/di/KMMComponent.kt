package io.customer.shared.di

import io.customer.shared.Platform
import io.customer.shared.sdk.config.CustomerIOConfig

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
    val platform: Platform,
    val sdkConfig: CustomerIOConfig,
) : DIGraph() {
    private val platformComponent: KMMPlatformComponent = KMMPlatformComponent(
        staticComponent = staticComponent,
        platform = platform,
    )
}
