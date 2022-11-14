package io.customer.shared.di

import io.customer.shared.sdk.config.BuildConfigurations
import io.customer.shared.sdk.config.getBuildConfigurations
import io.customer.shared.util.*

/**
 * Static component dependency graph to satisfy independent dependencies from single place. All
 * other graphs should never redefine dependencies available here unless extremely necessary. The
 * structure of DIGraphs would something like:
 *
 * -> StaticGraph (Independent)
 * --> PlatformGraph (Depends on StaticGraph and Platform)
 * ---> WorkspaceGraph (Depends on StaticGraph, PlatformGraph)
 *
 * The class should only contain dependencies matching the following criteria:
 * - dependencies that may be required without SDK initialization.
 * - dependencies that are lightweight and are not dependent on SDK initialization.
 */
@Suppress("MemberVisibilityCanBePrivate", "unused")
class KMMStaticComponent : DIGraph() {
    val buildConfigurations: BuildConfigurations
        get() = getNewInstance { getBuildConfigurations() }

    internal val dateTimeUtil: DateTimeUtil
        get() = getNewInstance { DateTimeUtilImpl() }

    internal val dispatcher: Dispatcher
        get() = getNewInstance { KMMDispatcher() }

    val logger: Logger
        get() = getSingletonInstance { ConsoleLogger(buildConfigurations = buildConfigurations) }
}
