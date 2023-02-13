package io.customer.shared.work

internal sealed class TimeUnit {
    abstract fun toMillis(): Long

    data class Milliseconds(private val value: Long) : TimeUnit() {
        override fun toMillis(): Long = value
        override fun toString(): String = "$value millis"

        companion object {
            fun fromSeconds(seconds: Double) = Milliseconds(value = (seconds * 1000.0).toLong())
        }
    }

    data class Seconds(private val value: Double) : TimeUnit() {
        override fun toMillis(): Long = Milliseconds.fromSeconds(seconds = value).toMillis()
        override fun toString(): String = "$value seconds"

        companion object {
            fun fromMillis(millis: Long) = Seconds(value = millis / 1000.0)
            fun fromDays(numDays: Int): Seconds {
                val secondsIn24Hours = 24.0 * 60.0 * 60.0
                return Seconds(value = numDays * secondsIn24Hours)
            }
        }
    }
}
