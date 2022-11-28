package io.customer.shared.util

import java.util.*

internal actual fun getPlatformUtil(): PlatformUtil = PlatformUtilImpl()

internal class PlatformUtilImpl : PlatformUtil {
    override fun generateUUID(): String = UUID.randomUUID().toString()
}
