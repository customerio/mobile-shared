package com.customerio.shared.database

import android.content.Context
import com.customerio.shared.local.CioDatabase
import com.squareup.sqldelight.android.AndroidSqliteDriver
import com.squareup.sqldelight.db.SqlDriver

actual class DatabaseDriverFactory(private val context: Context) {
    actual fun createDriver(): SqlDriver {
        return AndroidSqliteDriver(CioDatabase.Schema, context, "customerio.db")
    }
}