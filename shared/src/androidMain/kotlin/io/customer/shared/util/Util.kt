package io.customer.shared.util

import java.util.*

actual fun generateRandomUUID(): String = UUID.randomUUID().toString()
