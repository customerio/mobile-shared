package io.customer.shared.tracking.queue

import io.customer.shared.di.KMMComponent
import io.customer.shared.work.TimeUnit

internal expect val KMMComponent.queueTimer: Timer

/**
 * Wrapper around timer that gives us the ability to provide OS based timers and mocked timers in
 * tests to make them run faster.
 */
internal interface Timer {
    /**
     * Schedules a new timer and call given block after it has been completed.
     *
     * @param cancelPrevious true to force start new timer and cancel previous one; else starts new
     * timer only if no timer was running previously.
     * @param duration timer countdown duration.
     * @param block code block to be executed after timer is complete.
     */
    fun schedule(cancelPrevious: Boolean, duration: TimeUnit.Seconds, block: () -> Unit)
    fun cancel()
}
