package io.customer.shared.database

import io.customer.shared.local.CioDatabase
import com.squareup.sqldelight.EnumColumnAdapter
import local.TrackingTask

internal class DatabaseHelper(databaseDriverFactory: DatabaseDriverFactory) {
    private val database = CioDatabase(
        driver = databaseDriverFactory.createDriver(databaseName = DATABASE_NAME),
        trackingTaskAdapter = TrackingTask.Adapter(
            createdAtAdapter = DateTimeInstantAdapter,
            updatedAtAdapter = DateTimeInstantAdapter,
            expiresAtAdapter = DateTimeInstantAdapter,
            stalesAtAdapter = DateTimeInstantAdapter,
            identityTypeAdapter = EnumColumnAdapter(),
            queueTaskStatusAdapter = EnumColumnAdapter(),
            errorReasonAdapter = EnumColumnAdapter(),
        ),
    )
    val trackingTaskQueries = database.trackingTaskQueries

    companion object {
        const val DATABASE_NAME = "customerio.db"
    }
}
