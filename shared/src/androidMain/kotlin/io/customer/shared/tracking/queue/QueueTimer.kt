package io.customer.shared.tracking.queue

import android.os.CountDownTimer
import io.customer.shared.util.Logger
import io.customer.shared.work.JobDispatcher
import io.customer.shared.work.JobExecutor
import io.customer.shared.work.TimeUnit
import io.customer.shared.work.runOnMain

internal actual fun getQueueTimer(
    logger: Logger,
    jobExecutor: JobExecutor,
): QueueTimer = AndroidQueueTimer(
    logger = logger,
    jobExecutor = jobExecutor,
)

/**
 * [QueueTimer] implementation that uses Android [CountDownTimer] to run timer countdowns.
 */
private class AndroidQueueTimer(
    private val logger: Logger,
    override val jobExecutor: JobExecutor,
) : QueueTimer, JobDispatcher {
    @Volatile
    private var countdownTimer: CountDownTimer? = null
    private val timerAlreadyScheduled: Boolean
        get() = countdownTimer != null

    override fun onCoroutineFailed(exception: Throwable) {
        countdownTimer = null
        logger.error(message = "Timer failed with error ${exception.message}")
    }

    override fun schedule(force: Boolean, duration: TimeUnit.Seconds, block: () -> Unit) {
        synchronized(this) {
            if (!force && timerAlreadyScheduled) {
                log("already scheduled to run. Skipping request.")
            }
            scheduleAndCancelPrevious(duration = duration, block = block)
        }
    }

    private fun scheduleAndCancelPrevious(duration: TimeUnit, block: () -> Unit) {
        // Must create and start timer on the main UI thread or Android will throw an exception
        // saying the current thread doesn't have a Looper.
        // Because we are starting a new coroutine, there is a chance that there could be a delay
        // in starting the timer. This is OK because this function is designed to be async anyway
        // so the logic from the caller has not changed.
        runOnMain(
            onFailure = { ex ->
                unsafeCancel()
                logger.error(message = "Timer cannot be started, reason: ${ex.message}")
            },
        ) {
            synchronized(this) {
                unsafeCancel()

                log("making a timer for $duration")

                val millisInFuture = duration.toMillis()
                // Since we only need to listen at completion, using the same duration for
                // countdown interval
                countdownTimer = object : CountDownTimer(millisInFuture, millisInFuture) {
                    override fun onTick(millisUntilFinished: Long) {}
                    override fun onFinish() {
                        // reset timer before calling block as block might be synchronous and if it
                        // tries to start a new timer, it will not succeed because we need to reset
                        // the timer.
                        timerDone()
                        block()
                    }
                }.start()
            }
        }
    }

    private fun timerDone() {
        synchronized(this) {
            countdownTimer = null

            log("timer is done! It's been reset")
        }
    }

    override fun cancel() {
        synchronized(this) {
            log("timer is being cancelled")
            unsafeCancel()
        }
    }

    // cancel without having a mutex lock. Call within a synchronized{} block
    private fun unsafeCancel() {
        countdownTimer?.cancel()
        countdownTimer = null
    }

    private fun log(message: String) {
        logger.debug("QueueTimer $message")
    }
}
