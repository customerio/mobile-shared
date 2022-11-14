package io.customer.shared.database

import io.customer.shared.local.CioDatabase

internal class DatabaseHelper(databaseDriverFactory: DatabaseDriverFactory) {
    private val database = CioDatabase(
        driver = databaseDriverFactory.createDriver(databaseName = DATABASE_NAME),
    )
    private val dbQuery = database.cioDatabaseQueries

    companion object {
        const val DATABASE_NAME = "customerio.db"
    }
}
