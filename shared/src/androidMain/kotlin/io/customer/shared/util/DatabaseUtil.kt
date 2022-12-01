package io.customer.shared.util

import java.util.*

internal actual fun getDatabaseUtil(): DatabaseUtil = AndroidDatabaseUtil()

internal class AndroidDatabaseUtil : DatabaseUtil {
    override fun generateUUID(): String = UUID.randomUUID().toString()
}
