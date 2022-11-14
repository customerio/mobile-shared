package io.customer.shared.database

import com.squareup.sqldelight.db.SqlDriver
import io.customer.shared.Platform

/**
 * Database driver factory class to connect with platform specific SQL database.
 */
expect class DatabaseDriverFactory {
    /**
     * Creates driver for SQL database with given name.
     */
    fun createDriver(databaseName: String): SqlDriver
}

/**
 * Method allowing generation of platform specific [DatabaseDriverFactory].
 */
expect fun getDatabaseDriverFactory(platform: Platform): DatabaseDriverFactory
