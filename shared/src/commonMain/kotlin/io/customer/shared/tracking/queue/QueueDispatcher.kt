package io.customer.shared.tracking.queue

import io.customer.shared.database.TrackingTask
import io.customer.shared.database.TrackingTaskQueryHelper
import io.customer.shared.sdk.config.BackgroundQueueConfig
import io.customer.shared.tracking.api.*
import io.customer.shared.tracking.api.model.*
import io.customer.shared.tracking.constant.Priority
import io.customer.shared.tracking.constant.QueueTaskStatus
import io.customer.shared.util.DateTimeUtil
import io.customer.shared.util.Logger
import io.customer.shared.work.*
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
    private val queueTimer: QueueTimer,
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
        processBatchTasks(isRecursive = false) { getTasksToBatch() }
    }

    override suspend fun sendAllPending() {
        processBatchTasks(isRecursive = true) {
            trackingTaskQueryHelper.selectAllPendingTasks().getOrNull()
        }
    }

    /**
     * Since we have imposed a limit of maximum number of tasks, [isRecursive] allows the caller
     * to send multiple batch events. If true, it will keep calling [getTasks] after every
     * successful execution until an empty list of tasks is provided.
     */
    private fun processBatchTasks(
        isRecursive: Boolean,
        getTasks: suspend () -> List<TrackingTask>?,
    ) = runOnBackground(
        onFailure = { releaseLock() },
    ) {
        if (mutex.isLocked) return@runOnBackground

        acquireLock()
        kotlin.runCatching {
            queueTimer.cancel()
        }.onFailure { ex ->
            logger.error("Cannot cancel scheduled timer error: ${ex.message}")
        }
        kotlin.runCatching {
            do {
                val pendingTasks = getTasks()
                val isSuccess: Boolean = if (!pendingTasks.isNullOrEmpty()) {
                    queueRunner.runQueueForTasks(pendingTasks).getOrNull() == true
                } else false
            } while (isRecursive && isSuccess)
        }.onFailure { ex ->
            logger.error("Validation for pending tasks failed with error: ${ex.message}")
        }
        kotlin.runCatching {
            queueTimer.schedule(
                force = true,
                duration = TimeUnit.Seconds(backgroundQueueConfig.batchDelayMaxDelayInSeconds.toDouble()),
            ) { runOnBackground { checkForPendingTasks(QueueTriggerSource.TIMER) } }
        }.onFailure { ex ->
            logger.error("Cannot schedule timer error: ${ex.message}")
        }
        releaseLock()
    }

    private fun shouldSendImmediately(task: TrackingTask, timestamp: Instant): Boolean {
        val timeInQueue = timestamp.minus(task.createdAt).toLong(DurationUnit.SECONDS)
        return shouldCountInBatchTrigger(task) && (
                timeInQueue >= backgroundQueueConfig.batchDelayMaxDelayInSeconds
                        || task.priority > Priority.DEFAULT)
    }

    private fun shouldCountInBatchTrigger(task: TrackingTask): Boolean {
        return task.retryCount <= 0 || task.statusCode == null
    }

    @Throws(Exception::class)
    private suspend fun getTasksToBatch(): List<TrackingTask>? {
        val pendingTasks = trackingTaskQueryHelper.selectAllPendingTasks().getOrNull()
        if (pendingTasks.isNullOrEmpty()) return null

        var dispatch = false
        var pendingTasksCount = 0
        val currentTime = dateTimeUtil.now
        pendingTasks.forEach { task ->
            dispatch = dispatch || shouldSendImmediately(task = task, timestamp = currentTime)
            pendingTasksCount += if (shouldCountInBatchTrigger(task = task)) 1 else 0
        }
        dispatch = dispatch || pendingTasksCount >= backgroundQueueConfig.batchDelayMinTasks
        logger.debug("Total tasks checked: ${pendingTasks.size}, batching :$dispatch")

        if (dispatch) {
            trackingTaskQueryHelper.updateTasksStatus(
                status = QueueTaskStatus.SENDING,
                tasks = pendingTasks,
            )
        } else {
            // queue next
            trackingTaskQueryHelper.updateTasksStatus(
                status = QueueTaskStatus.PENDING,
                tasks = pendingTasks,
            )
        }
        return pendingTasks.takeIf { dispatch }
    }
}
