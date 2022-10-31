package io.customer.shared.database

import io.customer.shared.local.CioDatabase


internal class DataBaseHelper(databaseDriverFactory: DatabaseDriverFactory) {
    private val database = CioDatabase(databaseDriverFactory.createDriver())
    private val dbQuery = database.cioDatabaseQueries

}