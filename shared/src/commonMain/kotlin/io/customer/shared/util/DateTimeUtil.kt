package io.customer.shared.util

import kotlinx.datetime.Clock
import kotlinx.datetime.Instant

internal interface DateTimeUtil {
    val now: Instant

    fun toEpochSeconds(time: Instant): Long
    fun toEpochMilliseconds(time: Instant): Long
}

internal val DateTimeUtil.nowSeconds: Long
    get() = toEpochSeconds(time = this.now)

internal val DateTimeUtil.nowMilliseconds: Long
    get() = toEpochMilliseconds(time = this.now)

internal class DateTimeUtilImpl : DateTimeUtil {
    override val now: Instant
        get() = Clock.System.now()

    override fun toEpochSeconds(time: Instant): Long = time.epochSeconds
    override fun toEpochMilliseconds(time: Instant): Long = time.toEpochMilliseconds()
}
