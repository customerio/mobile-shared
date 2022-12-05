package io.customer.shared.util

import platform.Foundation.NSUUID

internal actual fun getDatabaseUtil(): DatabaseUtil = iOSDatabaseUtil()

@Suppress("ClassName")
internal class iOSDatabaseUtil : DatabaseUtil {
    override fun generateUUID(): String = NSUUID().UUIDString()
}
