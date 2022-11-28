package io.customer.shared.util

/**
 * Util class to hold helper functions that require platform specific implementation.
 */
internal interface PlatformUtil {
    /**
     * Generates unique UUID to uniquely identify database entries.
     */
    fun generateUUID(): String
}

internal expect fun getPlatformUtil(): PlatformUtil
