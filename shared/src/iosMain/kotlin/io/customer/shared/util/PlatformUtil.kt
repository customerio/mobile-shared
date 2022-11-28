package io.customer.shared.util

import platform.Foundation.NSUUID

internal actual fun getPlatformUtil(): PlatformUtil = PlatformUtilImpl()

internal class PlatformUtilImpl : PlatformUtil {
    override fun generateUUID(): String = NSUUID().UUIDString()
}
