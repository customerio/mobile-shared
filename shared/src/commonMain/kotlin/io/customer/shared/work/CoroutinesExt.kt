package io.customer.shared.work

import kotlinx.coroutines.Job

fun Job.cancelWithoutException() = try {
    cancel()
} catch (ex: Exception) {
    // Ignore, as the exception here is expected
}
