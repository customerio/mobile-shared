package io.customer.shared.database

import android.content.Context
import com.squareup.sqldelight.android.AndroidSqliteDriver
import com.squareup.sqldelight.db.SqlDriver
import io.customer.shared.AndroidPlatform
import io.customer.shared.Platform
import io.customer.shared.local.CioDatabase

actual class DatabaseDriverFactory(private val context: Context) {
    actual fun createDriver(databaseName: String): SqlDriver = AndroidSqliteDriver(
        schema = CioDatabase.Schema,
        context = context,
        name = databaseName,
    )
}

// Platform in Android classes will always be [AndroidPlatform]
actual fun getDatabaseDriverFactory(platform: Platform) = DatabaseDriverFactory(
    context = (platform as AndroidPlatform).applicationContext
)
