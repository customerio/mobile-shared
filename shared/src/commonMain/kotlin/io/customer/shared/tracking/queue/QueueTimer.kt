package io.customer.shared.tracking.queue

import io.customer.shared.util.Logger
import io.customer.shared.work.JobExecutor
import io.customer.shared.work.TimeUnit

internal expect fun getQueueTimer(
    logger: Logger,
    jobExecutor: JobExecutor,
): QueueTimer

/**
 * Wrapper around timer that gives us the ability to provide OS based timers and mocked timers in
 * tests to make them run faster.
 */
internal interface QueueTimer {
    /**
     * Schedules a new timer and call given block after it has been completed.
     *
     * @param force true to force start new timer and cancel previous one; else starts new timer
     * only if no timer was running previously.
     * @param duration timer countdown duration.
     * @param block code block to be executed after timer is complete.
     */
    fun schedule(force: Boolean, duration: TimeUnit.Seconds, block: () -> Unit)
    fun cancel()
}
