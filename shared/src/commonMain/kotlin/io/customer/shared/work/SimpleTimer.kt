package io.customer.shared.work

import io.customer.shared.util.Logger
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

interface QueueTimer {
    fun schedule(seconds: Int, block: suspend () -> Unit)
    fun cancel()
}

internal class CoroutineQueueTimer(
    private val logger: Logger,
    override val executor: CoroutineExecutor,
) : QueueTimer, CoroutineExecutable {
    var scheduledTimer: Job? = null
    override fun schedule(seconds: Int, block: suspend () -> Unit) {
        logger.debug("Cancelling previous timer")
        cancel()
        logger.debug("Scheduling new timer")
        scheduledTimer = runOnBackground {
            logger.debug("Timer scheduled for $seconds seconds")
            delay(timeMillis = seconds * 1_000L)
            if (isActive) {
                logger.debug("Scheduled timer wait complete")
                block()
                logger.debug("Scheduled timer execution complete")
            } else logger.debug("Timer already cancelled, skipping execution")
        }
    }

    override fun cancel() {
        scheduledTimer?.run {
            runOnMain {
                runCatching {
                    cancelAndJoin()
                }.onFailure { ex ->
                    logger.error("Timer could not be cancelled, reason: $ex")
                }
            }
        }
        scheduledTimer = null
    }
}
