package io.customer.shared.tracking.queue

import io.customer.shared.database.TrackingTask
import io.customer.shared.database.TrackingTaskQueryHelper
import io.customer.shared.sdk.config.BackgroundQueueConfig
import io.customer.shared.tracking.constant.Priority
import io.customer.shared.tracking.constant.QueueTaskStatus
import io.customer.shared.util.DateTimeUtil
import io.customer.shared.util.Logger
import io.customer.shared.work.JobDispatcher
import io.customer.shared.work.JobExecutor
import io.customer.shared.work.TimeUnit
import io.customer.shared.work.runOnBackground
import kotlinx.coroutines.sync.Mutex
import kotlinx.datetime.Instant
import kotlin.time.DurationUnit

/**
 * Class responsible for running queue operations. This class only receives requests to validate the
 * queue and is responsible for following:
 *
 * - whether to run the queue now or wait (except when specifically asked to run).
 * - maintain lock to avoid sending same tasks multiple times.
 * - validate and update task status related to queue operations.
 * - schedule timer for next queue request.
 */
internal interface QueueDispatcher {
    suspend fun checkForPendingTasks(source: QueueTriggerSource)
    suspend fun sendAllPending()
}

internal class QueueDispatcherImpl(
    private val logger: Logger,
    private val dateTimeUtil: DateTimeUtil,
    override val jobExecutor: JobExecutor,
    private val backgroundQueueConfig: BackgroundQueueConfig,
    private val trackingTaskQueryHelper: TrackingTaskQueryHelper,
    private val queueRunner: QueueRunner,
    private val queueTimer: Timer,
) : QueueDispatcher, JobDispatcher {
    private val mutex = Mutex()

    private suspend fun acquireLock() {
        logger.debug("Acquiring lock for queue")
        kotlin.runCatching {
            mutex.lock(owner = this)
        }.onFailure { ex ->
            logger.error("Mutex locking failed, reason: $ex")
        }
    }

    private fun releaseLock() {
        logger.debug("Releasing lock for queue")
        kotlin.runCatching {
            mutex.unlock()
        }.onFailure { ex ->
            logger.error("Mutex unlocking failed, reason: $ex")
        }
    }

    override fun onCoroutineFailed(exception: Throwable) {
        releaseLock()
    }

    override suspend fun checkForPendingTasks(source: QueueTriggerSource) {
        processBatchTasks(isRecursive = false) {
            val pendingTasks = getTasksToBatch()
            if (shouldDispatch(tasks = pendingTasks)) return@processBatchTasks pendingTasks
            else {
                // Revert tasks status so they can be batched with next run
                trackingTaskQueryHelper.updateTasksStatus(
                    status = QueueTaskStatus.PENDING,
                    tasks = pendingTasks,
                )
                return@processBatchTasks emptyList()
            }
        }
    }

    override suspend fun sendAllPending() {
        processBatchTasks(isRecursive = true) { getTasksToBatch() }
    }

    /**
     * Since we have imposed a limit of maximum number of tasks, [isRecursive] allows the caller
     * to send multiple batch events. If true, it will keep calling [getTasks] after every
     * successful execution until an empty list of tasks is provided.
     */
    private fun processBatchTasks(
        isRecursive: Boolean,
        getTasks: suspend () -> List<TrackingTask>,
    ) = runOnBackground {
        kotlin.runCatching {
            if (mutex.isLocked) return@runOnBackground

            acquireLock()
            queueTimer.cancel()
            kotlin.runCatching {
                do {
                    val pendingTasks = getTasks()
                    val isSuccess: Boolean = if (pendingTasks.isEmpty()) false
                    else queueRunner.runQueueForTasks(pendingTasks).getOrNull() == true
                } while (isRecursive && isSuccess)
            }.onFailure { ex ->
                logger.error("Validation for pending tasks failed with error: ${ex.message}")
            }
            queueTimer.schedule(
                cancelPrevious = true,
                duration = TimeUnit.Seconds(backgroundQueueConfig.batchDelayMaxDelayInSeconds.toDouble()),
            ) { runOnBackground { checkForPendingTasks(QueueTriggerSource.TIMER) } }
            releaseLock()
        }.onFailure { releaseLock() }
    }

    /**
     * Returns task eligible to batch and update their status to [QueueTaskStatus.QUEUED]. It is
     * callers responsibility to update next status as needed.
     *
     * @return list of tasks to batch, empty if no tasks available.
     */
    private suspend fun getTasksToBatch(): List<TrackingTask> {
        return trackingTaskQueryHelper.selectAllPendingTasks().getOrNull() ?: emptyList()
    }

    /**
     * Determines whether the provided tasks batch should be dispatched immediately or can wait.
     *
     * @param tasks to be evaluated.
     * @return true if the tasks should be dispatched immediately, false otherwise.
     */
    private fun shouldDispatch(tasks: List<TrackingTask>): Boolean {
        var dispatch = false
        var pendingTasksCount = 0
        val currentTime = dateTimeUtil.now
        tasks.forEach { task ->
            dispatch = dispatch || shouldSendImmediately(task = task, timestamp = currentTime)
            pendingTasksCount += if (shouldCountInBatchTrigger(task = task)) 1 else 0
        }
        dispatch = dispatch || pendingTasksCount >= backgroundQueueConfig.batchDelayMinTasks
        return dispatch
    }

    /**
     * Method to evaluate if a task is overdue can trigger queue run immediately. The task can be
     * trigger the batch only if it is counted as important (@see [shouldCountInBatchTrigger]) and
     * meets any of the following criteria:
     *
     * - Has higher priority than normal tasks.
     * - Has exceeded the maximum duration for any task to stay in queue.
     *
     * @param task to be evaluated.
     * @param timestamp current time at the start of method call.
     * @return true if the queue should be triggered instantly; false if it can wait.
     */
    private fun shouldSendImmediately(task: TrackingTask, timestamp: Instant): Boolean {
        if (!shouldCountInBatchTrigger(task)) return false

        if (task.priority > Priority.DEFAULT) return true
        val timeInQueue = timestamp.minus(task.createdAt).toLong(DurationUnit.SECONDS)
        return timeInQueue >= backgroundQueueConfig.batchDelayMaxDelayInSeconds
    }

    /**
     * Evaluates if a task can trigger a batch. This is mainly to avoid looping on invalid tasks.
     * If a task was sent previously, but was rejected by server for any reason (including server
     * down), we don't consider it important enough to trigger the run. This means the task will
     * stay in batch, but will not be counted as new task.
     *
     * e.g. With minimum tasks to batch as 10, if we have 11 tasks in total, and 2 of them being
     * re-attempted, we only consider remaining 9 tasks to be important enough to trigger the queue.
     * The re-attempted 2 tasks will be batched though and won't be skipped.
     *
     * This should only help in fixable tasks or when pausing queue.
     *
     * @param task to be evaluated.
     * @return true if the task meet the specified criteria; false otherwise.
     */
    private fun shouldCountInBatchTrigger(task: TrackingTask): Boolean {
        return task.retryCount <= 0 || task.statusCode == null
    }
}
