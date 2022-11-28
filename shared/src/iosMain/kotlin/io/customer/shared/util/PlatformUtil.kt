package io.customer.shared.util

import platform.Foundation.NSUUID

internal actual fun getPlatformUtil(): PlatformUtil = IOSPlatformUtil()

internal class IOSPlatformUtil : PlatformUtil {
    override fun generateUUID(): String = NSUUID().UUIDString()
}
