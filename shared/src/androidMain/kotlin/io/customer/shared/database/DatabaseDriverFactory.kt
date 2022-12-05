package io.customer.shared.database

import android.content.Context
import com.squareup.sqldelight.android.AndroidSqliteDriver
import com.squareup.sqldelight.db.SqlDriver
import io.customer.shared.Platform

actual class DatabaseDriverFactory(private val context: Context) {
    actual fun createDriver(databaseName: String): SqlDriver = AndroidSqliteDriver(
        schema = CioDatabase.Schema,
        context = context,
        name = databaseName,
    )
}

actual fun getDatabaseDriverFactory(platform: Platform) = DatabaseDriverFactory(
    context = platform.applicationContext,
)
