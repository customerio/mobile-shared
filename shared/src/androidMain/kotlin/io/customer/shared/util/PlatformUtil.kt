package io.customer.shared.util

import java.util.*

internal actual fun getPlatformUtil(): PlatformUtil = AndroidPlatformUtil()

internal class AndroidPlatformUtil : PlatformUtil {
    override fun generateUUID(): String = UUID.randomUUID().toString()
}
