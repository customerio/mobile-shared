package io.customer.shared.work

import io.customer.shared.util.Dispatcher
import io.customer.shared.util.Logger

/**
 * Wrapper around an OS timer that gives us the ability to mock timers in tests to make them run
 * faster.
 */
internal interface QueueTimer {
    fun schedule(force: Boolean, duration: TimeUnit.Seconds, block: suspend () -> Unit)
    fun cancel()
}

internal expect fun getQueueTimer(
    dispatcher: Dispatcher,
    logger: Logger,
    jobExecutor: JobExecutor,
): QueueTimer
