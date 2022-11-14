package io.customer.shared.database

import com.squareup.sqldelight.EnumColumnAdapter
import io.customer.shared.local.CioDatabase
import local.TrackingTask

internal class DatabaseHelper(databaseDriverFactory: DatabaseDriverFactory) {
    private val database = CioDatabase(
        driver = databaseDriverFactory.createDriver(databaseName = DATABASE_NAME),
        trackingTaskAdapter = TrackingTask.Adapter(
            createdAtAdapter = DateTimeInstantAdapter,
            expiresAtAdapter = DateTimeInstantAdapter,
            stalesAtAdapter = DateTimeInstantAdapter,
            queueTaskStatusAdapter = EnumColumnAdapter(),
            errorReasonAdapter = EnumColumnAdapter(),
        ),
    )
    val trackingTaskQueries = database.trackingTaskQueries

    companion object {
        const val DATABASE_NAME = "customerio.db"
    }
}
