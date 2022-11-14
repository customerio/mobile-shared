package io.customer.shared.database

import com.squareup.sqldelight.db.SqlDriver
import com.squareup.sqldelight.drivers.native.NativeSqliteDriver
import io.customer.shared.Platform
import io.customer.shared.local.CioDatabase

actual class DatabaseDriverFactory {
    actual fun createDriver(databaseName: String): SqlDriver {
        return NativeSqliteDriver(CioDatabase.Schema, databaseName)
    }
}

actual fun getDatabaseDriverFactory(platform: Platform) = DatabaseDriverFactory()
