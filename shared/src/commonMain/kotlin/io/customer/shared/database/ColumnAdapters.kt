package io.customer.shared.database

import com.squareup.sqldelight.ColumnAdapter
import kotlinx.datetime.Instant

/**
 * This file holds [ColumnAdapter] implementations required to store different types in SQLDelight
 * database.
 */

object DateTimeInstantAdapter : ColumnAdapter<Instant, Long> {
    override fun decode(databaseValue: Long): Instant {
        return Instant.fromEpochMilliseconds(databaseValue)
    }

    override fun encode(value: Instant): Long {
        return value.toEpochMilliseconds()
    }
}
