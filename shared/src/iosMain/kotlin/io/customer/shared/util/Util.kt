package io.customer.shared.util

import platform.Foundation.NSUUID

actual fun generateRandomUUID(): String = NSUUID().UUIDString()
