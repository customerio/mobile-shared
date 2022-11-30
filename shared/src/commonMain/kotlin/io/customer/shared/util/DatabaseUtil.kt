package io.customer.shared.util

/**
 * Util class to hold helper functions required by database classes.
 */
internal interface DatabaseUtil {
    /**
     * Generates unique UUID to uniquely identify database entries.
     */
    fun generateUUID(): String
}

internal expect fun getDatabaseUtil(): DatabaseUtil
