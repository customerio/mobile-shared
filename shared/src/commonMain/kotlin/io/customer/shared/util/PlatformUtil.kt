package io.customer.shared.util

internal interface PlatformUtil {
    fun generateUUID(): String
}

internal expect fun getPlatformUtil(): PlatformUtil
