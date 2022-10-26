package com.customerio.shared.database

import com.customerio.shared.local.CioDatabase

internal class DataBaseHelper(databaseDriverFactory: DatabaseDriverFactory) {
    private val database = CioDatabase(databaseDriverFactory.createDriver())
    private val dbQuery = database.cioDatabaseQueries

}