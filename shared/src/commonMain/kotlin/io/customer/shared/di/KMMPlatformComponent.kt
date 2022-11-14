package io.customer.shared.di

import io.customer.shared.Platform
import io.customer.shared.database.DatabaseDriverFactory
import io.customer.shared.database.DatabaseHelper
import io.customer.shared.database.getDatabaseDriverFactory

/**
 * Platform component dependency graph to satisfy platform based dependencies from single place.
 *
 * The class should only contain dependencies matching the following criteria:
 * - dependencies that requires platform classes to initialize (e.g. Android Context).
 * - dependencies that are should be defined/initialized once in SDK lifetime irrespective of
 * workspace changes.
 *
 * @see [KMMStaticComponent] for complete graph hierarchy.
 */
@Suppress("MemberVisibilityCanBePrivate", "unused")
internal class KMMPlatformComponent(
    private val staticComponent: KMMStaticComponent,
    val platform: Platform,
) : DIGraph() {
    private val databaseDriverFactory: DatabaseDriverFactory
        get() = getSingletonInstance { getDatabaseDriverFactory(platform = platform) }

    internal val databaseHelper: DatabaseHelper
        get() = getSingletonInstance { DatabaseHelper(databaseDriverFactory = databaseDriverFactory) }
}
