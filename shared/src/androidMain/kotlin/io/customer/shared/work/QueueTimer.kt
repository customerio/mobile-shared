package io.customer.shared.work

import android.os.CountDownTimer
import io.customer.shared.util.Dispatcher
import io.customer.shared.util.Logger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

internal actual fun getQueueTimer(
    dispatcher: Dispatcher,
    logger: Logger,
    jobExecutor: JobExecutor,
): QueueTimer = AndroidQueueTimer(
    dispatcher = dispatcher,
    logger = logger,
    executor = jobExecutor,
)

private class AndroidQueueTimer(
    private val dispatcher: Dispatcher,
    private val logger: Logger,
    override val executor: JobExecutor,
) : QueueTimer, JobDispatcher {
    @Volatile
    private var countdownTimer: CountDownTimer? = null
    private val timerAlreadyScheduled: Boolean
        get() = countdownTimer != null

    override fun schedule(force: Boolean, duration: TimeUnit.Seconds, block: suspend () -> Unit) {
        synchronized(this) {
            if (!force && timerAlreadyScheduled) {
                log("already scheduled to run. Skipping request.")
            }
            scheduleAndCancelPrevious(duration = duration, block = block)
        }
    }

    private fun scheduleAndCancelPrevious(duration: TimeUnit, block: suspend () -> Unit) {
        // Must create and start timer on the main UI thread or Android will throw an exception
        // saying the current thread doesn't have a Looper.
        // Because we are starting a new coroutine, there is a chance that there could be a delay
        // in starting the timer. This is OK because this function is designed to be async anyway
        // so the logic from the caller has not changed.
        CoroutineScope(dispatcher.main()).launch {
            countdownTimer = synchronized(this) {
                unsafeCancel()

                log("making a timer for $duration")

                val millisInFuture = duration.toMillis()
                object : CountDownTimer(millisInFuture, millisInFuture) {
                    override fun onTick(millisUntilFinished: Long) {}
                    override fun onFinish() {
                        // reset timer before calling block as block might be synchronous and if it
                        // tries to start a new timer, it will not succeed because we need to reset
                        // the timer.
                        timerDone()
                        runOnBackground { block() }
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
        logger.debug("Timer $message")
    }
}
