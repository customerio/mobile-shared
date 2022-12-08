package io.customer.shared.util

import kotlinx.datetime.Clock
import kotlinx.datetime.Instant

internal interface DateTimeUtil {
    val now: Instant
    val nowEpochSeconds: Long
    val nowEpochMilliseconds: Long

    fun toEpochSeconds(time: Instant): Long
    fun toEpochMilliseconds(time: Instant): Long
}

internal class DateTimeUtilImpl : DateTimeUtil {
    override val now: Instant
        get() = Clock.System.now()

    override val nowEpochSeconds: Long
        get() = toEpochSeconds(time = this.now)
    override val nowEpochMilliseconds: Long
        get() = toEpochMilliseconds(time = this.now)

    override fun toEpochSeconds(time: Instant): Long = time.epochSeconds
    override fun toEpochMilliseconds(time: Instant): Long = time.toEpochMilliseconds()
}
