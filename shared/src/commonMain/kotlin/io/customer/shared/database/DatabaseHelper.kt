package io.customer.shared.database

import com.squareup.sqldelight.EnumColumnAdapter

internal class DatabaseHelper(databaseDriverFactory: DatabaseDriverFactory) {
    private val database = CioDatabase(
        driver = databaseDriverFactory.createDriver(databaseName = DATABASE_NAME),
        trackingTaskAdapter = TrackingTask.Adapter(
            createdAtAdapter = DateTimeInstantAdapter,
            updatedAtAdapter = DateTimeInstantAdapter,
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
