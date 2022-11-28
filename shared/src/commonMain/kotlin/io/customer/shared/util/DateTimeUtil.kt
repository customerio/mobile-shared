package io.customer.shared.util

import kotlinx.datetime.Clock
import kotlinx.datetime.Instant

internal interface DateTimeUtil {
    val now: Instant
    val nowUnixTimestamp: Long

    /**
     * Returns unix timestamp in seconds.
     */
    fun toUnixTimestamp(time: Instant): Long
}

internal class DateTimeUtilImpl : DateTimeUtil {
    override val now: Instant
        get() = Clock.System.now()

    override val nowUnixTimestamp: Long
        get() = toUnixTimestamp(time = this.now)

    override fun toUnixTimestamp(time: Instant): Long {
        return time.epochSeconds
    }
}
